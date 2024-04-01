package skjsjhb.mc.hyaci.net

import skjsjhb.mc.hyaci.sys.Options
import skjsjhb.mc.hyaci.util.*
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * The status of a download task.
 */
enum class DownloadTaskStatus {
    /**
     * The task is created and ready to be scheduled.
     */
    READY,

    /**
     * The task is now active and transferring data.
     */
    ACTIVE,

    /**
     * The transfer has completed. No errors found.
     */
    DONE,

    /**
     * The file already exists and the action is skipped.
     */
    SKIPPED,

    /**
     * The task is canceled.
     */
    CANCELED,

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

    private var hostThread: Thread = Thread.currentThread()

    // There is no atomic version of ULong, but Long is still usually longer than any file length
    private var totalSize = AtomicLong(artifact.size().toLong())

    private var completedSize = AtomicLong(0)

    private val url = URI.create(artifact.url()).toURL()

    private val path = Path.of(artifact.path()).toAbsolutePath().normalize()

    private var tries = Options.getInt("downloads.tries", 3).clampMin(1)

    // Atomic wrapper of speed
    private var speed0 = AtomicLong(0)

    private var inputStream0: InputStream? = null

    private var outputStream0: OutputStream? = null

    private val canceled = AtomicBoolean(false)

    /**
     * Gets the speed of the current task.
     */
    fun speed(): Long = speed0.get()

    fun getTotalSize(): Long = totalSize.get()

    fun getCompletedSize(): Long = completedSize.get()

    /**
     * Checks whether the task has finished.
     */
    fun finished(): Boolean =
        status == DownloadTaskStatus.DONE || status == DownloadTaskStatus.SKIPPED || status == DownloadTaskStatus.FAILED

    /**
     * Resolves the download task, blocks until it's completed.
     */
    fun resolve(): Boolean {
        hostThread = Thread.currentThread()

        status = DownloadTaskStatus.ACTIVE
        info("Now $url")

        if (alreadyExists()) {
            info("Hit $url")
            status = DownloadTaskStatus.SKIPPED
            return true
        }

        while (tries > 0) {
            // Checks for cancellation
            if (canceled.get()) {
                status = DownloadTaskStatus.CANCELED
                warn("Cancelled $url")
                return false
            }

            // Perform a single try
            tries--
            try {
                retrieve()
                validateOrThrow()
                status = DownloadTaskStatus.DONE
                info("Got $url")
                return true
            } catch (e: FileNotFoundException) {
                // It's not likely to get solved by retrying
                warn("Resource at $url does not exist, skipped retries", e)
                break
            } catch (e: Exception) {
                if (canceled.get()) {
                    status = DownloadTaskStatus.CANCELED
                    warn("Cancelled $url")
                    return false
                }
                warn("Unable to download $url, $tries tries remain", e)
            }
        }
        status = DownloadTaskStatus.FAILED
        warn("Abandoned $url")
        return false
    }

    /**
     * Cancels the task.
     *
     * The cancel flag will first be set, then all active streams will be closed forcefully.
     * An [IOException] may throw when the task is canceled at the point of transferring data.
     */
    fun cancel() {
        canceled.set(true)

        // Forcefully close the streams
        inputStream0?.close()
        outputStream0?.close()
    }

    /**
     * Resolves the download, throws an exception if it failed.
     */
    fun resolveOrThrow() {
        if (!resolve()) throw IOException("Failed to download $url -> $path")
    }

    // Checks if the target file is already there
    private fun alreadyExists(): Boolean =
        if (!Files.exists(path)) false
        else {
            completedSize.set(Files.size(path)) // Trick the validator to use file size
            debug("Found existing file, validating")
            validate()
        }

    // Retrieves the content, but does not perform any validation.
    private fun retrieve() {
        totalSize.set(artifact.size().toLong())
        completedSize.set(0)

        url.openConnection().run {
            connect()
            debug("Connected to ${url.host}:${url.port.takeIf { it != -1 } ?: url.defaultPort}")

            if (canceled.get()) {
                throw IOException("Canceled")
            }

            if (contentLengthLong > 0) {
                totalSize.set(contentLengthLong)
                debug("Content length is $totalSize")
            } else {
                debug("Content length unknown")
            }

            debug("Start transferring stream data")

            inputStream0 = getInputStream()

            Files.createDirectories(path.parent)

            getInputStream().use { input ->
                Files.newOutputStream(
                    path,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE
                ).let { // The meter will close the backed stream on close
                    outputStream0 = it
                    MeteredOutputStream(it) {
                        completedSize.set(it.bytesTransferred)
                        speed0.set(it.speed)
                    }
                }.use { output -> input.transferTo(output) }
            }
        }
        if (!canceled.get()) {
            debug("Transfer ended, $completedSize bytes received")
        } else {
            // Normally when the stream is closed, an exception may be thrown
            // If it did not and this line is executed, then we throw one manually
            throw IOException("Cancel flag set before transfer completes")
        }
    }

    // Validates the file and throw an exception if failed
    private fun validateOrThrow() {
        if (!validate()) throw IOException("Unable to validate file")
    }

    // Validates the file
    // Validation based on option `downloads.validation`
    private fun validate(): Boolean =
        debug("Validating $url").let {
            when (Options.getString("downloads.validation", "checksum")) {
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
                checksum(artifact.path(), algo)
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

/**
 * Manages a set of download tasks and provide a unified interface to manage progress and speed.
 *
 * The Status of each task can be retrieved via this field after a call to [resolve] or [resolveOrThrow].
 *
 * @param artifacts Artifacts to handle.
 */
class DownloadGroup(artifacts: Iterable<Artifact>) : Progressed {
    private val tasks: List<DownloadTask> =
        artifacts.map { DownloadTask(it) }

    private val executorService =
        Executors.newWorkStealingPool(Options.getInt("downloads.poolSize", 32).clampMin(1))

    private var progressHandler: ((String, Double) -> Unit)? = null

    private val isActive = AtomicBoolean(false)

    /**
     * Gets the current speed.
     */
    fun speed(): Long = tasks.fold(0L) { s, t -> s + t.speed() }

    /**
     * Cancels this group of tasks, stopping new tasks from being started and tries to stop existing tasks.
     *
     * Any I/O operation that is not completed will stop, leaving possibly corrupted files.
     * This may also trigger an exception on any blocking [resolve] and [resolveOrThrow].
     *
     * Blocks until all active tasks are stopped.
     */
    fun cancel() {
        tasks.forEach { it.cancel() }
        executorService.shutdownNow()
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)
    }

    /**
     * Resolves the download group.
     *
     * This method blocks until all tasks are processed, either successful or failed.
     *
     * @return Whether all underlying tasks have succeeded.
     */
    fun resolve(): Boolean {
        setupProgressSync()
        return executorService.invokeAll(tasks.map {
            Callable {
                it.resolve()
            }
        }).all { it.get() }.also {
            executorService.shutdown()
            isActive.set(false)
        }
    }

    /**
     * Resolves the download group.
     * Throws an exception if any underlying task fails.
     *
     * This method blocks until all tasks are processed, either successful or failed.
     */
    fun resolveOrThrow() {
        setupProgressSync()
        val futures = executorService.invokeAll(tasks.map {
            Callable {
                it.resolveOrThrow()
            }
        })
        executorService.shutdown()
        futures.forEach { it.get() }
        isActive.set(false)
    }

    private fun setupProgressSync() {
        val progressUpdateInterval = Duration.ofMillis(250)

        isActive.set(true)
        Thread {
            while (isActive.get()) {
                val current = tasks.fold(0L) { s, t -> s + t.getCompletedSize() }
                val total = tasks.fold(0L) { s, t -> s + t.getTotalSize() }
                val s = toReadableSize(current) + " / " + toReadableSize(total)
                val p = current.toDouble() / total
                progressHandler?.invoke(s, p)
                Thread.sleep(progressUpdateInterval)
            }
        }.start()
    }

    override fun setProgressHandler(handler: (status: String, progress: Double) -> Unit) {
        progressHandler = handler
    }

    private fun toReadableSize(src: Long): String =
        if (src > 1024 * 1024 * 1024) String.format("%.2f GiB", src / 1024.0 / 1024.0 / 1024.0)
        else if (src > 1024 * 1024) String.format("%.2f MiB", src / 1024.0 / 1024.0)
        else if (src > 1024) String.format("%.2f KiB", src / 1024.0)
        else "$src B"
}

/**
 * Calculates the percentage value of the pair (first / second).
 */
fun Pair<Number, Number>.toValue(): Double =
    if (second.toDouble() == 0.0) Double.NaN else first.toDouble() / second.toDouble()

// String comparison ignoring case
private fun String.equalsIgnoreCase(other: String?): Boolean = lowercase() == other?.lowercase()

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
