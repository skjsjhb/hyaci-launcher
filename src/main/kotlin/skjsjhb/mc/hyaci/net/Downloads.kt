package skjsjhb.mc.hyaci.net

import skjsjhb.mc.hyaci.sys.Options
import skjsjhb.mc.hyaci.util.*
import java.io.IOException
import java.io.InterruptedIOException
import java.io.OutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.NoSuchAlgorithmException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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

    /**
     * Gets the speed of the current task.
     */
    fun speed(): Long = speed0.get()

    /**
     * Gets the progress of the current task.
     */
    fun progress(): Pair<Long, Long> = Pair(completedSize.get(), totalSize.get())

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
            // Harmony interrupt check
            if (Thread.currentThread().isInterrupted) {
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
            } catch (e: InterruptedIOException) {
                status = DownloadTaskStatus.CANCELED
                warn("Cancelled $url", e)
                return false
            } catch (e: Exception) {
                warn("Unable to download $url, $tries tries remain", e)
            }
            // And try again
        }
        status = DownloadTaskStatus.FAILED
        warn("Abandoned $url")
        return false
    }

    /**
     * Cancels the task by interrupting the host thread.
     *
     * Cancellation will happen during the string transformation.
     * Interrupts are not accepted when connecting or validating.
     */
    fun cancel() {
        if (hostThread == Thread.currentThread()) {
            warn("Cancellation from the same thread is usually unwanted")
        }
        hostThread.interrupt()
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
            if (contentLengthLong > 0) {
                totalSize.set(contentLengthLong)
                debug("Content length is $totalSize")
            } else {
                debug("Content length unknown")
            }

            debug("Start transferring stream data")

            getInputStream().use { input ->
                Files.newOutputStream(
                    path,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE
                ).let { // The meter will close the backed stream on close
                    MeteredOutputStream(it) {
                        completedSize.set(it.bytesTransferred)
                        speed0.set(it.speed)
                    }
                }.let { // The monitor will close the backed stream on close
                    InterruptibleOutputStream(it)
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
        info("Validating $url").let {
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

/**
 * Manages a set of download tasks and provide a unified interface to manage progress and speed.
 *
 * The Status of each task can be retrieved via this field after a call to [resolve] or [resolveOrThrow].
 *
 * @param artifacts Artifacts to handle.
 */
class DownloadGroup(artifacts: Set<Artifact>) {
    val tasks: Set<DownloadTask> =
        mutableSetOf<DownloadTask>()
            .apply { artifacts.forEach { add(DownloadTask(it)) } }
            .also { debug("Created download group of ${it.size} tasks") }

    private val executorService =
        Executors.newWorkStealingPool(Options.getInt("downloads.poolSize", 32).clampMin(1))

    /**
     * Gets the current speed.
     */
    fun speed(): Long = tasks.fold(0L) { s, t -> s + t.speed() }

    /**
     * Cancels this group of tasks, stopping new tasks from being started and tries to stop existing tasks.
     *
     * Tasks are stopped by interrupting the host thread of the task.
     * Any I/O operation that is not completed will stop, leaving possibly corrupted files.
     * They might be overridden on retries.
     *
     * May trigger an exception on any blocking [resolve] and [resolveOrThrow].
     *
     * Blocks until all active tasks are stopped.
     */
    fun cancel() {
        executorService.shutdownNow()
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)
    }

    /**
     * Gets the progress in terms of the completed tasks.
     */
    fun progressByCount(): Pair<Int, Int> =
        Pair(tasks.fold(0) { s, t -> if (t.finished()) s + 1 else s }, tasks.size)

    /**
     * Gets the progress in terms of the completed size.
     */
    fun progressBySize(): Pair<Long, Long> {
        var cs = 0L
        var ts = 0L
        tasks.map { it.progress() }.forEach {
            cs += it.first
            ts += it.second
        }
        return Pair(cs, ts)
    }

    /**
     * Resolves the download group.
     *
     * This method blocks until all tasks are processed, either successful or failed.
     *
     * @return Whether all underlying tasks have succeeded.
     */
    fun resolve(): Boolean =
        executorService.invokeAll(tasks.map { Callable { it.resolve() } }).all { it.get() }
            .also { executorService.shutdown() }

    /**
     * Resolves the download group.
     * Throws an exception if any underlying task fails.
     *
     * This method blocks until all tasks are processed, either successful or failed.
     */
    fun resolveOrThrow() {
        val futures = executorService.invokeAll(tasks.map { Callable { it.resolveOrThrow() } })
        executorService.shutdown()
        futures.forEach { it.get() }
    }
}

/**
 * Calculates the percentage of the pair (first / second).
 */
fun Pair<Number, Number>.asPercentage(): Double =
    if (second.toDouble() == 0.0) Double.NaN else first.toDouble() / second.toDouble()

// String comparison ignoring case
private fun String.equalsIgnoreCase(other: String?): Boolean = this.lowercase() == other?.lowercase()

// Interval of sampling
private const val meterSampleInterval = 100

// Throws and exception when the current thread is interrupted
private class InterruptibleOutputStream(private val origin: OutputStream) : OutputStream() {
    private fun throwIfInterrupted() {
        if (Thread.currentThread().isInterrupted) throw InterruptedIOException("Interrupted")
    }

    override fun write(b: Int) {
        throwIfInterrupted()
        origin.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        throwIfInterrupted()
        origin.write(b, off, len)
    }

    override fun write(b: ByteArray) {
        throwIfInterrupted()
        origin.write(b)
    }

    override fun close() {
        origin.close()
        super.close()
    }
}

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
