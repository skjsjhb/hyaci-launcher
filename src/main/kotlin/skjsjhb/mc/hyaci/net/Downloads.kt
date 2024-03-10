package skjsjhb.mc.hyaci.net

import skjsjhb.mc.hyaci.sys.Options
import skjsjhb.mc.hyaci.util.*
import java.io.IOException
import java.io.OutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.NoSuchAlgorithmException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong

// Configure the pool size of the downloader
private val dlTaskPool: ExecutorService by lazy {
    val parallelism = Options.getInt("downloads.poolSize", 32).clampMin(1)
    debug("Downloader pool size set to $parallelism")
    Executors.newWorkStealingPool(parallelism)
}

/**
 * The status of a download task.
 */
enum class DownloadTaskStatus {
    /**
     * The task is created and ready to be scheduled.
     */
    READY,

    /**
     * The task has been committed to the pool, but not yet started.
     */
    COMMITTED,

    /**
     * The task is now active and transferring data.
     */
    ACTIVE,

    /**
     * The transfer has completed. No errors found.
     */
    DONE,

    /**
     * The transfer has failed, either an I/O exception occurred, or the validation did not pass.
     */
    FAILED
}

/**
 * A class which holds runtime data of a download task.
 */
class DownloadTask(private val artifact: Artifact) {
    /**
     * The [DownloadTaskStatus] of the current task.
     */
    var status: DownloadTaskStatus = DownloadTaskStatus.READY
        @Synchronized
        get
        @Synchronized
        set

    // There is no atomic version of ULong, but Long is still usually longer than any file length
    private var totalSize = AtomicLong(artifact.size().toLong())

    private var completedSize = AtomicLong(0)

    private val url = URI.create(artifact.url()).toURL()

    private val path = Path.of(artifact.path()).toAbsolutePath().normalize()

    private var tries = Options.getInt("downloads.tries", 3).clampMin(1)

    // Atomic wrapper of speed
    private var speed0 = AtomicLong(0)

    /**
     * Gets the speed of the current task.
     */
    fun speed(): Long = speed0.get()

    /**
     * Gets the progress of the current task.
     *
     * Value ranges from `0.0` to `1.0`.
     * If the task is not started, the value is `0.0`.
     * If it has failed, the value is the progress of the last successful byte.
     * A value lower than `0.0` means the progress is unknown.
     */
    fun progress(): Double = if (totalSize.get() <= 0) -1.0 else completedSize.get().toDouble() / totalSize.get()

    /**
     * Checks whether the task has finished, either completed or failed.
     */
    fun finished(): Boolean = status == DownloadTaskStatus.DONE || status == DownloadTaskStatus.FAILED

    /**
     * Commits the task to be downloaded.
     */
    fun resolve(): Future<Boolean> =
        dlTaskPool.submit(Callable { download() })
            .also {
                status = DownloadTaskStatus.COMMITTED
                info("Committed $url -> $path")
            }

    // Try download and validate once
    private fun download(): Boolean {
        status = DownloadTaskStatus.ACTIVE
        info("Now $url")

        while (tries > 0) {
            tries--
            runCatching {
                retrieve()
                validateOrThrow()
                status = DownloadTaskStatus.DONE
                info("Got $url")
                return true
            }.onFailure { warn("Unable to download $url, $tries tries remain", it) } // And try again
        }
        status = DownloadTaskStatus.FAILED
        warn("Abandoned $url")
        return false
    }

    // Retrieves the content, but does not perform any validation.
    private fun retrieve() {
        totalSize.set(artifact.size().toLong())
        completedSize.set(0)

        url.openConnection().run {
            connect()
            debug("Connected to ${url.host}:${url.port.takeIf { it != -1 } ?: url.defaultPort}")

            if (contentLengthLong > 0) {
                totalSize.set(contentLengthLong)
                debug("Content length is $totalSize")
            } else {
                debug("Content length unknown")
            }

            debug("Start transferring stream data")

            inputStream.use { input ->
                Files.newOutputStream(
                    path,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE
                ).let {
                    MeteredOutputStream(it) {
                        completedSize.set(it.bytesTransferred)
                        speed0.set(it.speed)
                    }
                }.use { output -> input.transferTo(output) }
            }
        }

        debug("Transfer ended, $completedSize bytes received")
    }

    // Validates the file and throw an exception if failed
    private fun validateOrThrow() {
        if (!validate()) throw IOException("Unable to validate file")
    }

    // Validates the file
    // Validation based on option `downloads.validation`
    private fun validate(): Boolean =
        when (Options.getString("downloads.validation", "checksum")) {
            "checksum" -> validateChecksum()
            "size" -> validateSize()
            else -> true
        }.also { info("Validating $url") }

    private fun validateChecksum(): Boolean {
        artifact.checksum().ifBlank {
            debug("Missing checksum, switching to size validation")
            return validateSize()
        }.let {
            val (algo, expected) = it.split("=")
            val actual = try {
                checksumOf(artifact.path(), algo)
            } catch (e: NoSuchAlgorithmException) {
                warn("Unsupported algorithm $algo, switching to size validation", e)
                return validateSize()
            }

            return if (expected.equalsIgnoreCase(actual)) {
                debug("Checksum validated ($expected)")
                true
            } else {
                warn("Checksum mismatch, expected $expected but received $actual)")
                false
            }
        }
    }

    private fun validateSize(): Boolean = when {
        totalSize.get() <= 0 -> debug("Unable to validate size, total size unknown").let { true }
        completedSize.get() == totalSize.get() -> debug("Size validated ($completedSize)").let { true }
        else -> warn("Size mismatch, expected $totalSize but received $completedSize)").let { false }
    }
}

// String comparison ignoring case
private fun String.equalsIgnoreCase(other: String?): Boolean = this.lowercase() == other?.lowercase()

// Interval of sampling
private const val meterSampleInterval = 100

// Meters the transferred bytes of a stream
private class MeteredOutputStream(
    private val outStream: OutputStream,
    private val onUpdate: (it: MeteredOutputStream) -> Any?
) : OutputStream() {
    var bytesTransferred = 0L
        private set

    var speed = 0L // Bytes per second
        private set

    private var nextMeterTime = System.currentTimeMillis() + meterSampleInterval
    private var lastMeterBytes = 0L

    private fun internalOnTransfer(s: Int = 1) {
        bytesTransferred += s
        updateSpeedMeter()
        onUpdate(this)
    }

    private fun updateSpeedMeter() {
        System.currentTimeMillis().let {
            if (it > nextMeterTime) {
                speed = (bytesTransferred - lastMeterBytes) * 1000 / (it - nextMeterTime + meterSampleInterval)
                lastMeterBytes = bytesTransferred
                nextMeterTime += meterSampleInterval
            }
        }
    }

    override fun write(b: Int) {
        outStream.write(b)
        internalOnTransfer()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        outStream.write(b, off, len)
        internalOnTransfer(len)
    }

    override fun write(b: ByteArray) {
        outStream.write(b)
        internalOnTransfer(b.size)
    }

    override fun close() {
        super.close()
        outStream.close()
    }
}
