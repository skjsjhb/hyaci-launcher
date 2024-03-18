package skjsjhb.mc.hyaci.install

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import skjsjhb.mc.hyaci.launch.JsonLaunchProfile
import skjsjhb.mc.hyaci.launch.LaunchProfile
import skjsjhb.mc.hyaci.launch.accepts
import skjsjhb.mc.hyaci.net.Artifact
import skjsjhb.mc.hyaci.net.DownloadGroup
import skjsjhb.mc.hyaci.net.Requests
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.util.*
import skjsjhb.mc.hyaci.vfs.Vfs
import java.nio.file.Files

/**
 * Installs a vanilla game.
 *
 * @param id The ID of the game.
 * @param fs The [Vfs] to install the game on.
 */
class VanillaInstaller(private val id: String, private val fs: Vfs) : Installer {
    // Cached profile object
    private lateinit var profile: LaunchProfile

    override fun install() {
        info("Installing $id on ${fs.resolve(".")}")

        fetchProfile()
        fetchFiles()
        postInstall()

        info("Installed $id")
    }

    private fun fetchProfile() {
        val profileUrl = versionManifest.gets("versions")
            ?.jsonArray?.find { it.getString("id") == id }?.getString("url")
            ?: throw NoSuchElementException("No profile with ID $id")

        debug("Profile url is $profileUrl")

        val profileContent = Requests.getString(profileUrl)
        profile = JsonLaunchProfile(Json.parseToJsonElement(profileContent))

        fs.profile(id).let {
            Files.createDirectories(it.parent)
            Files.writeString(it, profileContent)
        }

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
                fs.assetIndex(it.path()).let {
                    Files.createDirectories(it.parent)
                    Files.writeString(it, assetIndexContent)
                }

                if (mapToResources) {
                    // Save a copy for ancient versions
                    fs.assetMapToResources("${profile.assetId()}.json").let {
                        Files.createDirectories(it.parent)
                        Files.writeString(it, assetIndexContent)
                    }
                }

                debug("Saved asset index ${it.path()}")

                // Generate assets
                assetIndexObject.gets("objects")?.let {
                    it.jsonObject.entries.forEach { (fileName, v) ->
                        val hash = v.getString("hash")
                        val size = v.getLong("size")
                        val url = Sources.VANILLA_RESOURCES.value + "/${hash.substring(0..1)}/$hash"

                        val path = when {
                            mapToResources -> fs.assetMapToResources(fileName)
                            isLegacy -> fs.assetLegacy(fileName)
                            else -> fs.asset(hash)
                        }

                        add(Artifact.of(url, path.toString(), size.toULong(), "sha1=$hash"))
                    }
                }
            }

            // Libraries
            profile.libraries().filter {
                // Among all profiles, there is no library whose rules involve feature keys
                // An OS-based value set should be enough
                it.rules() accepts osRuleValues
            }.forEach {
                listOfNotNull(it.artifact(), it.nativeArtifact())
                    .forEach {
                        add(
                            Artifact.of(
                                it.url(),
                                fs.library(it.path()).toString(),
                                it.size(),
                                it.checksum()
                            )
                        )
                    }
            }

            // Client
            profile.clientArtifact()?.run {
                add(
                    Artifact.of(
                        url(),
                        fs.client(profile.id()).toString(),
                        size(),
                        checksum()
                    )
                )
            }

            // Logging
            profile.loggingArtifact()?.run {
                add(
                    Artifact.of(
                        url(),
                        fs.logConfig(path()).toString(),
                        size(),
                        checksum()
                    )
                )
            }
        }.let {
            info("Fetching files for ${profile.id()} (${it.size})")
            DownloadGroup(it).resolveOrThrow()
        }
    }

    // Runs post-install tasks
    private fun postInstall() {
        info("Running post-install tasks")
        // Unpack natives
        debug("Unpacking optional native libraries")
        profile.libraries()
            .filter { it.rules() accepts osRuleValues }
            .mapNotNull { it.nativeArtifact() }
            .forEach {
                debug("Unpacking ${it.path()}")
                unzip(fs.library(it.path()).toString(), fs.natives(profile.id()).toString())
            }
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