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
private val dlTaskPool: ExecutorService = run {
    val parallelism = Options.getInt("downloads.poolSize", 32).clampMin(1)
    debug("Downloader pool size set to $parallelism")
    Executors.newWorkStealingPool(parallelism)
}

/**
 * A class which holds runtime data of a download task.
 */
class DownloadTask(private val artifact: Artifact) {
    // There is no atomic version of ULong, but Long is still usually longer than any file length
    private var totalSize = AtomicLong(artifact.size().toLong())

    private var completedSize = AtomicLong(0)

    private val url = URI.create(artifact.url()).toURL()

    private val path = Path.of(artifact.path()).toAbsolutePath().normalize()

    private var tries = Options.getInt("downloads.tries", 3).clampMin(1)

    /**
     * Commits the task to be downloaded.
     */
    fun resolve(): Future<Boolean> =
        dlTaskPool.submit(Callable { download() }).also { info("Committed $url -> $path") }

    // Try download and validate once
    private fun download(): Boolean {
        info("Now $url")
        while (tries > 0) {
            tries--
            try {
                retrieve()
                validateOrThrow()
            } catch (e: Exception) {
                warn("Unable to download $url, $tries tries remain", e)
                continue
            }
            info("Got $url")
            return true
        }
        warn("Abandoned $url")
        return false
    }

    // Retrieves the content, but does not perform any validation.
    private fun retrieve() {
        totalSize.set(artifact.size().toLong())
        completedSize.set(0)

        val connection = url.openConnection()

        connection.connect()
        debug("Connected to ${url.host}:${url.port.takeIf { it != -1 } ?: url.defaultPort}")

        connection.contentLengthLong.let {
            if (it > 0) {
                totalSize.set(connection.contentLengthLong)
                debug("Content length is $totalSize")
            } else {
                debug("Content length unknown")
            }
        }

        debug("Start transferring stream data")

        connection.inputStream.use { input ->
            Files.newOutputStream(
                path,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE
            )
                .let { MeteredOutputStream(it) { completedSize.set(bytesTransferred) } }
                .use { output -> input.transferTo(output) }
        }

        totalSize.get().let { if (it > 0) it else "?" }.let {
            debug("Transfer ended, $completedSize bytes received")
        }
    }

    // Validates the file and throw an exception if failed
    private fun validateOrThrow() {
        if (!validate()) throw IOException("Unable to validate file")
    }

    // Validates the file
    // Validation based on option `downloads.validation`
    private fun validate(): Boolean {
        info("Validating $url")
        return when (Options.getString("downloads.validation", "checksum")) {
            "checksum" -> validateChecksum()
            "size" -> validateSize()
            else -> true
        }
    }

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
    private val onUpdate: MeteredOutputStream.() -> Any?
) : OutputStream() {
    var bytesTransferred = 0L
        private set

    var speed = 0L // Bytes per second
        private set

    private var nextMeterTime = System.currentTimeMillis() + meterSampleInterval
    private var lastMeterBytes = 0L

    private fun internalOnTransfer(s: Int = 1) {
        bytesTransferred += s
        onUpdate()
    }

    private fun updateSpeedMeter() {
        System.currentTimeMillis().let {
            if (it > nextMeterTime) {
                speed = bytesTransferred * 1000 / (it - nextMeterTime)
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
