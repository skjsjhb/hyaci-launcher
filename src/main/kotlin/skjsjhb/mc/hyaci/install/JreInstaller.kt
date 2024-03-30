package skjsjhb.mc.hyaci.install

import skjsjhb.mc.hyaci.launch.JreManager
import skjsjhb.mc.hyaci.net.Artifact
import skjsjhb.mc.hyaci.net.DownloadGroup
import skjsjhb.mc.hyaci.net.Requests
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.sys.Options
import skjsjhb.mc.hyaci.sys.dataPathOf
import skjsjhb.mc.hyaci.util.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * An installer which installs the specified JRE component.
 */
class JreInstaller(private val componentName: String) : Installer {
    private lateinit var files: List<JreFile>
    private val rootDir = getBundledJreInstallDir(componentName)

    override fun install() {
        info("Installing runtime $componentName")

        retrieveFileList()
        fetchFiles()
        inflateLZMA()
        makeExecutable()
        registerComponent()

        info("Installed runtime $componentName")
    }

    private fun registerComponent() {
        JreManager.put(componentName, getBundledJreBinaryPath(componentName).toString())
    }

    private fun fetchFiles() {
        files.map {
            Artifact.of(
                it.artifact.url(),
                rootDir.resolve(it.artifact.path() + if (it.isLzma) ".lzma" else "").toString(),
                it.artifact.size(),
                it.artifact.checksum()
            )
        }.let {
            debug("Downloading ${it.size} files")
            DownloadGroup(it).resolveOrThrow()
        }
    }

    private fun inflateLZMA() {
        debug("Inflating LZMA files")

        // Inflate concurrently
        Executors.newWorkStealingPool().use {
            it.invokeAll(
                files.filter { it.isLzma }.map {
                    Callable {
                        val pat = rootDir.resolve(it.artifact.path()).toString()
                        unlzma("$pat.lzma", pat)
                        Files.deleteIfExists(Path.of("$pat.lzma"))
                        debug("Inflated $pat")
                    }
                }
            ).forEach { it.get() }
            it.shutdown()
        }
    }

    private fun makeExecutable() {
        debug("Making files executable")
        files.filter { it.executable }.forEach {
            rootDir.resolve(it.artifact.path()).let {
                it.toFile().setExecutable(true)
                debug("Executable flag set: $it")
            }
        }
    }

    private fun retrieveFileList() {
        val componentKey = "${osPair()}.$componentName"
        val manifestUrl = jreManifest.getString("$componentKey.0.manifest.url")
            .ifBlank { throw UnsupportedOperationException("No manifest found for $componentKey ") }

        // LZMA is not really faster, hence we leave an option here
        val lzmaEnabled = Options.getBoolean("installer.jre.lzma", false)

        files = Requests.getJson(manifestUrl).getObject("files").orEmpty()
            .filterKeys { !it.startsWith("legal/") && !it.startsWith("jre.bundle/Contents/Home/legal/") }
            .filterValues { it.getString("type") == "file" }
            .map { (k, v) ->
                val isLzma = lzmaEnabled && v.getString("downloads.lzma.url").isNotBlank()
                val typeHint = if (isLzma) "lzma" else "raw"
                val artifact = Artifact.of(
                    v.getString("downloads.$typeHint.url"),
                    k,
                    v.getLong("downloads.$typeHint.url").toULong(),
                    "sha1=" + v.getString("downloads.$typeHint.sha1")
                )

                JreFile(artifact, v.getBoolean("executable"), isLzma)
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

private fun getBundledJreInstallDir(componentName: String): Path = dataPathOf("runtimes/$componentName")

private fun getBundledJreBinaryPath(componentName: String): Path = getBundledJreInstallDir(componentName).resolve(
    when (Canonical.osName()) {
        "osx" -> "jre.bundle/Contents/Home/bin/java"
        "windows" -> "bin/java.exe"
        else -> "bin/java"
    }
)

private data class JreFile(
    val artifact: Artifact,
    val executable: Boolean,
    val isLzma: Boolean
)

private val jreManifest by lazy {
    Requests.getJson(Sources.MOJANG_JRE_MANIFEST.value)
}