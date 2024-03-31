package skjsjhb.mc.hyaci.install

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import skjsjhb.mc.hyaci.container.Container
import skjsjhb.mc.hyaci.net.Artifact
import skjsjhb.mc.hyaci.net.DownloadGroup
import skjsjhb.mc.hyaci.net.Requests
import skjsjhb.mc.hyaci.profile.ConcreteProfile
import skjsjhb.mc.hyaci.profile.JsonProfile
import skjsjhb.mc.hyaci.profile.filterRules
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.util.*
import java.nio.file.Files
import java.nio.file.Path

/**
 * Installs a vanilla game.
 *
 * @param id The ID of the game.
 * @param container The [Container] to install the game on.
 */
class VanillaInstaller(private val id: String, private val container: Container) : Installer, Progressed {
    // Cached profile object
    private lateinit var profile: ConcreteProfile

    private var progressHandler: ((String, Double) -> Unit)? = null

    override fun install() {
        info("Installing $id on ${container.resolve(".")}")

        fetchProfile()
        fetchFiles()
        postInstall()

        info("Installed $id")
    }

    private fun fetchProfile() {
        progressHandler?.invoke("Fetch Profile", -1.0)

        val profileUrl = versionManifest.getArray("versions")
            ?.find { it.getString("id") == id }
            ?.getString("url")
            ?: throw NoSuchElementException("No profile with ID $id")

        debug("Profile url is $profileUrl")

        val profileContent = Requests.getString(profileUrl)
        profile = ConcreteProfile(JsonProfile(Json.parseToJsonElement(profileContent)), container)

        container.profile(id).let {
            Files.createDirectories(it.parent)
            Files.writeString(it, profileContent)
        }

        progressHandler?.invoke("Fetch Profile", 1.0)

        debug("Saved profile")
    }

    // Fetches files of the given profile.
    private fun fetchFiles() {
        mutableSetOf<Artifact>().apply {
            // Asset index and assets
            profile.assetIndexArtifact()?.let {
                debug("Resolving asset index ${it.path()}")
                val assetIndexContent = Requests.getString(it.url())
                val assetIndexObject = Json.parseToJsonElement(assetIndexContent)

                // Handles pre-1.6 assets
                val mapToResources = assetIndexObject.getBoolean("map_to_resources")
                val isLegacy = profile.assetId() == "pre-1.6" || profile.assetId() == "legacy"

                // The asset index will be resolved at the post-installation stage
                Path.of(it.path()).let {
                    Files.createDirectories(it.parent)
                    Files.writeString(it, assetIndexContent)
                }

                if (mapToResources) {
                    // Save a copy for ancient versions
                    container.assetMapToResources("${profile.assetId()}.json").let {
                        Files.createDirectories(it.parent)
                        Files.writeString(it, assetIndexContent)
                    }
                }

                debug("Saved asset index ${it.path()}")

                // Generate assets
                assetIndexObject.getObject("objects")?.forEach { fileName, v ->
                    val hash = v.getString("hash")
                    val size = v.getLong("size")
                    val url = Sources.VANILLA_RESOURCES.value + "/${hash.substring(0..1)}/$hash"

                    val path = when {
                        mapToResources -> container.assetMapToResources(fileName)
                        isLegacy -> container.assetLegacy(fileName)
                        else -> container.asset(hash)
                    }

                    add(Artifact.of(url, path.toString(), size.toULong(), "sha1=$hash"))
                }
            }

            // Libraries
            profile.libraries()
                // Among all profiles, there is no library whose rules involve feature keys
                // An OS-based value set should be enough
                .filterRules(osRuleValues)
                .flatMap { listOfNotNull(it.artifact(), it.nativeArtifact()) }
                .let { addAll(it) }

            // Client
            profile.clientArtifact()?.let { add(it) }

            // Logging
            profile.loggingArtifact()?.let { add(it) }
        }.let {
            info("Fetching files for ${profile.id()} (${it.size})")
            DownloadGroup(it).run {
                setProgressHandler { status, progress ->
                    progressHandler?.invoke("Fetch Files ($status)", progress)
                }
                resolveOrThrow()
            }
        }
    }

    // Runs post-install tasks
    private fun postInstall() {
        info("Running post-install tasks")
        // Unpack natives
        debug("Unpacking optional native libraries")
        profile.libraries()
            .filterRules(osRuleValues)
            .mapNotNull { it.nativeArtifact() }
            .withProgress { _, progress -> progressHandler?.invoke("Unpack Natives", progress) }
            .forEach {
                debug("Unpacking ${it.path()}")
                unzip(it.path(), container.natives(profile.id()).toString())
            }
    }

    override fun setProgressHandler(handler: (status: String, progress: Double) -> Unit) {
        progressHandler = handler
    }
}

private val osRuleValues by lazy {
    mapOf(
        "os.name" to Canonical.osName(),
        "os.arch" to Canonical.osArch(),
        "os.version" to Canonical.osVersion()
    )
}

private val versionManifest: JsonElement by lazy {
    info("Synchronizing version manifest")
    Json.parseToJsonElement(Requests.getString(Sources.VANILLA_VERSION_MANIFEST.value)).also {
        info("Version manifest updated")
    }
}