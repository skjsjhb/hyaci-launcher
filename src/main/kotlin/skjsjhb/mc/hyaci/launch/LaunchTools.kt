package skjsjhb.mc.hyaci.launch

import kotlinx.serialization.json.Json
import skjsjhb.mc.hyaci.auth.Account
import skjsjhb.mc.hyaci.util.debug
import skjsjhb.mc.hyaci.util.getBoolean
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
 */
data class LaunchPack(
    val id: String,
    val fs: Vfs,
    val rv: Map<String, String>,
    val account: Account,
    val java: String
)

// The limitation of backlog buffer
private const val backlogLimit = 10000

/**
 * Represents a running game instance.
 */
class Game(private val launchPack: LaunchPack) {
    private val builder = ProcessBuilder()

    // Process instance
    private var process: Process? = null

    // A queue to store logs
    private val logBuffer: Queue<String> = ConcurrentLinkedQueue()

    // Cached profile
    private val profile = LaunchProfile.load(launchPack.id, launchPack.fs)

    // Path to Java executable
    private val javaPath = JreManager.get(launchPack.java.ifBlank { profile.jreComponent() })

    /**
     * Accesses the log buffer via a shared [Stream].
     *
     * This method always returns a reference to the same stream object.
     * This is not thread-safe by default and should be synchronized.
     */
    val logs: Stream<String?> = Stream.generate { logBuffer.poll() }

    /**
     * Stops the process.
     *
     * Usually this is not meant to be called, as the game is usually closed by the player.
     * However, if the game failed to launch while the process is dangling, this method might be needed.
     */
    fun stop() {
        process?.apply {
            warn("Killing game process ${pid()} (this might cause data loss)")
            destroy()
        }
    }

    /**
     * Starts the game.
     *
     * Loads resources, creates the process, and returns immediately.
     */
    fun start() {
        if (process != null) {
            throw IllegalStateException("A process has already been created")
        }
        info("Starting using $javaPath")
        prepare()
        process = builder.start()
        info("Created game process ${process?.pid()}")
        forwardOutput()
    }

    /**
     * Waits for the process to exit and get its exit code.
     *
     * Returns immediately if already exited.
     */
    fun join(): Int {
        process?.let {
            return it.waitFor()
        }
        throw IllegalStateException("Joining a null process")
    }

    // Add log output to backlog buffer
    private fun commitLog(s: String) {
        while (logBuffer.size > backlogLimit) logBuffer.poll()
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
                name = "GameStat-${pid}"
                isDaemon = true
            }.start()
        }
    }

    /**
     * Decides whether the assets should be mapped to the resource folder.
     */
    private fun shouldAssetMap(): Boolean =
        Json.parseToJsonElement(Files.readString(launchPack.fs.assetIndex(profile.assetId())))
            .getBoolean("map_to_resources")

    // Assemble arguments and apply template values
    private fun createCommand(): List<String> {
        val variableMap = launchPack.run {
            mapOf(
                "version_name" to profile.version(),
                "game_directory" to fs.gameDir().toString(),
                "assets_root" to fs.assetRoot().toString(),
                "game_assets" to (if (shouldAssetMap()) fs.assetRootMapToResources() else fs.assetRootLegacy()).toString(),
                "assets_index_name" to profile.assetId(),
                "user_type" to "mojang",
                "version_type" to profile.versionType(),
                "natives_directory" to fs.natives(profile.id()).toString(),
                "classpath" to createClassPath(),
                "path" to fs.logConfig(profile.loggingArtifact()?.path() ?: "").toString(),
                "auth_player_name" to account.username(),
                "auth_uuid" to account.uuid(),
                "auth_session" to account.token(),
                "auth_access_token" to account.token(),
                "auth_xuid" to account.xuid(),
                "user_properties" to "[]", // 1.7 Twitch compatibility
                "clientid" to UUID.randomUUID().toString()
            )
        }

        return mutableListOf<String>().apply {
            add(javaPath)
            profile.jvmArguments().filter { it.rules() accepts launchPack.rv }.forEach { addAll(it.values()) }
            add(profile.mainClass())
            profile.gameArguments().filter { it.rules() accepts launchPack.rv }.forEach { addAll(it.values()) }
        }.map {
            // Apply templates
            variableMap.entries.fold(it) { acc, (k, v) -> acc.replace("\${$k}", v) }
        }
    }

    // Prepares for the run
    private fun prepare() {
        builder
            .command(createCommand())
            .directory(File(launchPack.fs.gameDir().toString()))
            .redirectErrorStream(true)
    }

    // Generates classpath
    private fun createClassPath(): String =
        launchPack.run {
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
            }.also { debug("Generated classpath for ${profile.id()}, length ${it.size}") }
                .joinToString(File.pathSeparator)
        }
}

