package skjsjhb.mc.hyaci.launch

import skjsjhb.mc.hyaci.util.debug
import skjsjhb.mc.hyaci.util.info
import skjsjhb.mc.hyaci.util.warn
import skjsjhb.mc.hyaci.vfs.Vfs
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.stream.Stream

/**
 * A summary of resources configured to launch the game.
 *
 * @param id The ID of the profile to launch.
 * @param fs The virtual filesystem for path resolution.
 * @param rv Values used to match the rules.
 * @param java Path to java executable.
 */
data class LaunchPack(
    val id: String,
    val fs: Vfs,
    val rv: Map<String, String>,
    val java: String
)

// The limitation of backlog buffer
private const val backlogLimit = 10000
private const val backlogClearAmount = 100

/**
 * Represents a running game instance.
 */
class Game(private val launchPack: LaunchPack) {
    private val builder = ProcessBuilder()

    // Process instance
    private var process: Process? = null

    // A queue to store logs
    private val logBuffer: Queue<String> = ConcurrentLinkedQueue()

    private val logStream: Stream<String?> = Stream.generate { logBuffer.poll() }

    /**
     * Accesses the log buffer via a shared [Stream].
     *
     * This method always returns a reference to the same stream object.
     * This is not thread-safe by default and should be synchronized.
     */
    fun logs(): Stream<String?> = logStream

    /**
     * Creates the game process, starts it, and waits for it to exit.
     */
    fun run() {
        start()
        process?.waitFor()
    }

    /**
     * Stops the process.
     *
     * Usually this is not meant to be called, as the game is usually closed by the player.
     * However, if the game failed to launch while the process is dangling, this method might be needed.
     */
    fun stop() {
        process?.run {
            warn("Killing game process ${pid()} (this might cause data loss)")
            destroy()
        }
    }

    // Creates the process
    private fun start() {
        if (process == null) {
            info("Starting using ${launchPack.java}")
            prepare()
            process = builder.start()
            info("Created game process ${process?.pid()}")
            forwardOutput()
        } else {
            throw IllegalStateException("A process has already been created")
        }
    }

    // Add log output to backlog buffer
    private fun commitLog(s: String) {
        while (logBuffer.size > backlogLimit) {
            repeat(backlogClearAmount) { logBuffer.poll() }
        }
        logBuffer.offer(s)
    }

    // Binds listeners and continually pulls data from stdout and stderr.
    private fun forwardOutput() {
        val pid = process?.pid()
        process?.inputReader()?.let {
            Thread {
                debug("Output pipe attached")
                runCatching {
                    it.forEachLine { commitLog(it) } // Equals to continuous reading
                    debug("Output pipe closed")
                }.onFailure { warn("Unexpected I/O pipe exception", it) }
            }.apply {
                name = "Logcat (${pid})"
                isDaemon = true
            }.start()
        }
    }

    // Generate a command list
    private fun createCommand(): List<String> {
        val profile = loadLaunchProfile(launchPack.id) {
            Files.readString(launchPack.fs.profile(it))
        }
        return listOf(launchPack.java) + assembleArguments(launchPack, profile)
    }

    // Prepares for the run
    private fun prepare() {
        builder
            .command(createCommand())
            .directory(File(launchPack.fs.gameDir().toString()))
            .redirectErrorStream(true)
    }
}

// Assemble arguments and apply template values
private fun assembleArguments(lp: LaunchPack, profile: LaunchProfile): List<String> {
    val variableMap = with(lp) {
        mapOf(
            "version_name" to profile.version(),
            "game_directory" to fs.gameDir().toString(),
            "assets_root" to fs.assetRoot().toString(),
            "assets_index_name" to profile.assetId(),
            "user_type" to "mojang",
            "version_type" to profile.versionType(),
            "natives_directory" to fs.natives(profile.id()).toString(),
            "classpath" to createClassPath(lp, profile),
            "path" to fs.logConfig(profile.loggingArtifact()?.path() ?: "").toString(),
            "auth_player_name" to "Player", // TODO authenticate
            "auth_uuid" to UUID.nameUUIDFromBytes("OfflinePlayer:Player".toByteArray()).toString()
        )
    }

    return mutableListOf<String>().apply {
        profile.jvmArguments().filter { it.rules() accepts lp.rv }.forEach { addAll(it.values()) }
        add(profile.mainClass())
        profile.gameArguments().filter { it.rules() accepts lp.rv }.forEach { addAll(it.values()) }
    }.map {
        // Apply templates
        variableMap.entries.fold(it) { acc, (k, v) -> acc.replace("\${$k}", v) }
    }
}

// Generates classpath
private fun createClassPath(lp: LaunchPack, profile: LaunchProfile): String =
    with(lp) {
        mutableListOf<String>().apply {
            // Add libraries
            profile.libraries().forEach {
                if (it.rules() accepts rv) {
                    it.artifact()?.let {
                        add(fs.library(it.path()).toString())
                    }
                }
            }

            // Add client
            profile.clientArtifact()?.let {
                // Client artifacts do not have ID fields, use version field here for Forge compatibility
                add(fs.client(profile.version()).toString())
            }
        }.also { debug("Generated classpath for ${profile.id()}, length ${it.size}") }.joinToString(File.pathSeparator)
    }