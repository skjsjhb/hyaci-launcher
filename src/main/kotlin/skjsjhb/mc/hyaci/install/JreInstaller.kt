package skjsjhb.mc.hyaci.install

import skjsjhb.mc.hyaci.net.Artifact
import skjsjhb.mc.hyaci.net.DownloadGroup
import skjsjhb.mc.hyaci.net.Requests
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.sys.dataPathOf
import skjsjhb.mc.hyaci.util.*

/**
 * An installer which installs the specified JRE component.
 */
class JreInstaller(private val componentName: String) : Installer {
    override fun install() {
        info("Installing runtime $componentName")
        val rootDir = dataPathOf("runtimes/$componentName")
        getFiles().let { files ->
            files.map {
                Artifact.of(
                    it.artifact.url(),
                    rootDir.resolve(it.artifact.path()).toString(),
                    it.artifact.size(),
                    it.artifact.checksum()
                )
            }.toSet().let {
                debug("Runtime contains ${it.size} files")
                DownloadGroup(it).resolveOrThrow()
            }

            debug("Making files executable")
            files.filter { it.executable }.forEach {
                rootDir.resolve(it.artifact.path()).toFile().setExecutable(true)
            }
        }

        info("Installed runtime $componentName")
    }

    private fun getFiles(): Set<JreFile> {
        val componentKey = "${osPair()}.$componentName"
        val manifestUrl = jreManifest.getArray(componentKey)?.get(0)?.getString("manifest.url")
            ?: throw UnsupportedOperationException("No manifest found for $componentKey ")

        return mutableSetOf<JreFile>().apply {
            Requests.getJson(manifestUrl).getObject("files")?.forEach { k, v ->
                // Strip documents
                if (k.startsWith("legal/") || k.startsWith("jre.bundle/Contents/Home/legal/")) {
                    return@forEach
                }

                // Exclude links and directories
                if (v.getString("type") != "file") {
                    return@forEach
                }

                // Generate file
                val isLzma = v.getString("downloads.lzma.url").isNotBlank()
                // val typeHint = if (isLzma) "lzma" else "raw"
                val typeHint = "raw" // TODO support lzma
                val artifact = Artifact.of(
                    v.run { getString("downloads.$typeHint.url") },
                    k,
                    v.getLong("downloads.$typeHint.url").toULong(),
                    "sha1=" + v.getString("downloads.$typeHint.sha1")
                )
                add(JreFile(artifact, v.getBoolean("executable"), isLzma))
            }
        }
    }

    /**
     * Gets the OS value pair (`name[-arch]`).
     *
     * Value is one of `linux`, `mac-os`, `mac-os-arm64`, `windows-x64`. `linux-i386` and `windows-x86` are not
     * supported by hyaci.
     */
    private fun osPair(): String =
        when (Canonical.osName()) {
            "linux" -> "linux"
            "osx" -> if (Canonical.isArm()) "mac-os-arm64" else "mac-os"
            "windows" -> "windows-x64"
            else -> "" // This is not going to happen
        }
}

private data class JreFile(
    val artifact: Artifact,
    val executable: Boolean,
    val isLzma: Boolean
)

private val jreManifest by lazy {
    Requests.getJson(Sources.MOJANG_JRE_MANIFEST.value)
}