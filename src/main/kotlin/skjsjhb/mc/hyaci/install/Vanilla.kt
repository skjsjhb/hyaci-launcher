package skjsjhb.mc.hyaci.install

import kotlinx.serialization.json.*
import skjsjhb.mc.hyaci.launch.JsonLaunchProfile
import skjsjhb.mc.hyaci.launch.LaunchProfile
import skjsjhb.mc.hyaci.launch.accepts
import skjsjhb.mc.hyaci.net.Artifact
import skjsjhb.mc.hyaci.net.DownloadGroup
import skjsjhb.mc.hyaci.net.artifactOf
import skjsjhb.mc.hyaci.net.retrieveString
import skjsjhb.mc.hyaci.sys.canonicalOSArch
import skjsjhb.mc.hyaci.sys.canonicalOSName
import skjsjhb.mc.hyaci.sys.canonicalOSVersion
import skjsjhb.mc.hyaci.util.*
import skjsjhb.mc.hyaci.vfs.Vfs
import java.nio.file.Files

/**
 * Installs a vanilla game on the specified [Vfs].
 */
fun installVanilla(id: String, fs: Vfs) {
    info("Installing $id on ${fs.resolve(".")}")

    val profileUrl = versionManifest.gets("versions")
        ?.jsonArray?.find { it.getString("id") == id }?.getString("url")
        ?: throw NoSuchElementException("No profile with ID $id")

    debug("Profile url is $profileUrl")

    val profileContent = retrieveString(profileUrl)
    val profile: LaunchProfile = JsonLaunchProfile(Json.parseToJsonElement(profileContent))

    fs.profile(id).let {
        Files.createDirectories(it.parent)
        Files.writeString(it, profileContent)
    }

    debug("Saved profile")

    fetchFiles(profile, fs)

    postInstall(profile, fs)

    info("Installed $id")
}

private val osRuleValues by lazy {
    mapOf(
        "os.name" to canonicalOSName(),
        "os.arch" to canonicalOSArch(),
        "os.version" to canonicalOSVersion()
    )
}

// Runs post-install tasks
private fun postInstall(profile: LaunchProfile, fs: Vfs) {
    // Unpack natives
    info("Unpacking optional native libraries")
    profile.libraries()
        .filter { it.rules() accepts osRuleValues }
        .mapNotNull { it.nativeArtifact() }
        .forEach {
            debug("Unpacking ${it.path()}")
            unzip(fs.library(it.path()).toString(), fs.natives(profile.id()).toString())
        }
}

// Fetches files of the given profile.
private fun fetchFiles(profile: LaunchProfile, fs: Vfs) {
    mutableSetOf<Artifact>().apply {
        // Asset index and assets
        profile.assetIndexArtifact()?.let {
            debug("Resolving asset index ${it.path()}")
            val assetIndexContent = retrieveString(it.url())
            val assetIndexObject = Json.parseToJsonElement(assetIndexContent)

            // Handles pre-1.6 assets
            val mapToResources = assetIndexObject.gets("map_to_resources")?.jsonPrimitive?.boolean == true
            val isLegacy = profile.assetId() == "pre-1.6"

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
                    val size = v.gets("size")?.jsonPrimitive?.int ?: 0
                    val url = Sources.VANILLA_RESOURCES.asString() + "/${hash.substring(0..1)}/$hash"

                    val path = when {
                        mapToResources -> fs.assetMapToResources(fileName)
                        isLegacy -> fs.assetLegacy(fileName)
                        else -> fs.asset(hash)
                    }

                    add(artifactOf(url, path.toString(), size.toULong(), "sha1=$hash"))
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
                        artifactOf(
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
                artifactOf(
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
                artifactOf(
                    url(),
                    fs.logConfig(path()).toString(),
                    size(),
                    checksum()
                )
            )
        }
    }.let {
        info("Fetching files of ${profile.id()}")
        DownloadGroup(it).resolveOrThrow()
    }
}

private val versionManifest: JsonElement by lazy {
    info("Synchronizing version manifest")
    Json.parseToJsonElement(retrieveString(Sources.VANILLA_VERSION_MANIFEST.asString())).also {
        info("Version manifest updated")
    }
}