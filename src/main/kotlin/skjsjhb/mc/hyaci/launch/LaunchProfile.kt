package skjsjhb.mc.hyaci.launch

import kotlinx.serialization.json.*
import skjsjhb.mc.hyaci.net.Artifact
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.util.*
import skjsjhb.mc.hyaci.vfs.Vfs
import java.nio.file.Files

/**
 * Holding properties of game profiles for launching.
 *
 * Due to historical reasons, the game contains formats which are not compatible with each other.
 * This interface tries to provide a standard approach of retrieving properties without the need of investigating the
 * detailed format information.
 */
interface LaunchProfile {
    /**
     * ID of the profile.
     */
    fun id(): String

    /**
     * Game version name.
     */
    fun version(): String

    /**
     * Libraries of the profile.
     */
    fun libraries(): List<Library>

    /**
     * Gets the dependency of this profile.
     */
    fun inheritsFrom(): String

    /**
     * Gets the JVM arguments.
     *
     * Arguments for logging and other extra JVM arguments are also included.
     */
    fun jvmArguments(): List<Argument>

    /**
     * Gets the game arguments.
     */
    fun gameArguments(): List<Argument>

    /**
     * Gets the main class name.
     */
    fun mainClass(): String

    /**
     * Gets the asset ID.
     */
    fun assetId(): String

    /**
     * Gets the asset index artifact.
     */
    fun assetIndexArtifact(): Artifact?

    /**
     * Gets the logging artifact.
     */
    fun loggingArtifact(): Artifact?

    /**
     * Gets the name of JRE component.
     */
    fun jreComponent(): String

    /**
     * Gets the suggested JRE version.
     */
    fun jreVersion(): Int

    /**
     * Gets the client artifact.
     */
    fun clientArtifact(): Artifact?

    /**
     * Gets the client mappings artifact.
     */
    fun clientMappingsArtifact(): Artifact?

    /**
     * Gets the version type.
     */
    fun versionType(): String

    companion object LaunchProfileUtils {

        /**
         * Loads a launch profile from given [Vfs] instance.
         *
         * Profiles are parsed and linked.
         * Dependencies are fetched from the given [Vfs].
         *
         * @param id The ID of the profile to be loaded.
         * @param fs A [Vfs] filesystem containing the profile.
         */
        fun load(id: String, fs: Vfs): LaunchProfile = load(id) { Files.readString(fs.profile(it)) }

        /**
         * Loads a set of profiles using given getter.
         *
         * Profiles are parsed and linked.
         * Dependencies are fetched using the specified getter method.
         *
         * @param id The ID of the profile to be loaded.
         * @param getter A getter for retrieving profile content by the given ID.
         */
        fun load(id: String, getter: (id: String) -> String): LaunchProfile {
            info("Loading profile $id")

            var currentDep = id
            val visitedDep = mutableSetOf<String>()

            var head: LaunchProfile? = null

            // Repeatedly links head -> currentDep
            while (currentDep.isNotBlank()) {
                debug("Parsing profile $currentDep")

                // Detect circular dependency
                if (currentDep in visitedDep) {
                    warn("Circular dependency detected, dependency $currentDep, early returning.")
                    break
                }

                visitedDep.add(currentDep)

                // Link profile
                val base = JsonLaunchProfile(Json.parseToJsonElement(getter(currentDep)))
                head = linkProfile(base, head)
                currentDep = base.inheritsFrom()
            }
            return head!!
        }
    }
}

/**
 * Represents metadata of a library.
 */
interface Library {
    /**
     * Name of the library.
     */
    fun name(): String

    /**
     * Rules of this library.
     */
    fun rules(): List<Rule>

    /**
     * Gets the artifact of the library.
     */
    fun artifact(): Artifact?

    /**
     * Gets the native artifact of the library.
     */
    fun nativeArtifact(): Artifact?
}

/**
 * Generic argument representation.
 */
interface Argument {
    /**
     * The values of the argument when it's applied.
     */
    fun values(): List<String>

    /**
     * Rules associated.
     */
    fun rules(): List<Rule>
}

/**
 * An implementation of [LaunchProfile] based on vanilla JSON format.
 */
class JsonLaunchProfile(private val src: JsonElement) : LaunchProfile {
    override fun id(): String = src.getString("id")

    // This implementation does not give the correct result when multi-inheritance happens. This can be resolved during
    // the linking process, where the version eventually "falls into" a Mojang qualified version.
    override fun version(): String {
        // Compatibility with some merged profiles
        src.gets("patches")?.let {
            it.jsonArray
                .find { it.getString("id") == "game" }
                ?.getString("version")?.let { if (it.isNotBlank()) return it }
        }

        // Infer the version
        return src.getString("inheritsFrom", id())
    }

    override fun libraries(): List<Library> {
        val libs = src.gets("libraries")?.jsonArray ?: return emptyList()
        return libs.map { JsonLibrary(it) }
    }

    override fun inheritsFrom(): String = src.gets("inheritsFrom")?.jsonPrimitive?.content ?: ""

    private fun genArguments(name: String): List<Argument> =
        src.gets("arguments.$name")?.run { jsonArray.map { JsonArgument(it) } } ?: emptyList()

    private fun loggingArgument(): Argument? {
        return src.gets("logging.client.argument")?.let {
            object : Argument {
                override fun values(): List<String> = listOf(it.jsonPrimitive.content)
                override fun rules(): List<Rule> = emptyList()
            }
        }
    }

    override fun jvmArguments(): List<Argument> =
        mutableListOf<Argument>().apply {
            addAll(
                if (src.jsonObject.contains("arguments")) {
                    genArguments("jvm")
                } else {
                    // A stripped set of JVM arguments from modern profiles is ported to support pre-1.13 versions.
                    getFallbackJvmArguments()
                }
            )
            loggingArgument()?.let { add(it) }
        }

    override fun gameArguments(): List<Argument> =
        src.gets("minecraftArguments")?.run {
            jsonPrimitive.content.split(" ").map {
                object : Argument {
                    override fun values(): List<String> = listOf(it)
                    override fun rules(): List<Rule> = emptyList()
                }
            }
        } ?: genArguments("game")

    override fun mainClass(): String = src.getString("mainClass")

    override fun assetId(): String = src.getString("assets")

    override fun assetIndexArtifact(): Artifact? = src.gets("assetIndex")?.let { JsonArtifact(it) }

    override fun loggingArtifact(): Artifact? = src.gets("logging.client.file")?.let { JsonArtifact(it) }

    override fun jreComponent(): String = src.getString("javaVersion.component")

    override fun jreVersion(): Int = src.gets("javaVersion.majorVersion")?.jsonPrimitive?.int ?: 0

    override fun clientArtifact(): Artifact? = src.gets("downloads.client")?.let { JsonArtifact(it) }

    override fun clientMappingsArtifact(): Artifact? = src.gets("downloads.client_mappings")?.let { JsonArtifact(it) }

    override fun versionType(): String = src.getString("type")
}

private const val fallbackJvmArgsFile = "/fallback-jvm-args.json"

private fun getFallbackJvmArguments(): List<Argument> =
    object {}.javaClass.getResource(fallbackJvmArgsFile)?.readText()
        ?.let { Json.parseToJsonElement(it).jsonArray.map { JsonArgument(it) } } ?: emptyList()

/**
 * An implementation of [Library] based on vanilla JSON format.
 */
class JsonLibrary(private val src: JsonElement) : Library {
    override fun name(): String = src.getString("name")

    override fun rules(): List<Rule> = src.gets("rules")?.jsonArray?.map { JsonRule(it) } ?: emptyList()

    override fun artifact(): Artifact? {
        // Try existing
        src.gets("downloads.artifact")?.let { return JsonArtifact(it) }

        // Generate by name
        var url = src.getString("url").ifBlank { return null }

        val nameList = name().split(":").also { if (it.size < 3) return null }
        var (groupId, artifactId, version) = nameList
        groupId = groupId.replace(".", "/")

        val classifier = nameList.getOrNull(3)
        if (!url.endsWith("/")) url += "/"

        var path = "$groupId/$artifactId/$version/$artifactId-$version"

        classifier?.let { path += "-$it" }

        path += ".jar"
        url += path

        return object : Artifact {
            override fun url(): String = url
            override fun path(): String = path
            override fun size(): ULong = 0UL
            override fun checksum(): String = ""
        }
    }

    override fun nativeArtifact(): Artifact? {
        var nativesId = src.getString("natives.${Canonical.osName()}").ifBlank { return null }

        // 32-bit is dead now, assume 64-bit for native libraries
        nativesId = nativesId.replace("\${arch}", "64")
        return src.gets("downloads.classifiers.$nativesId")?.let { JsonArtifact(it) }
    }
}

/**
 * An implementation of [Artifact] based on vanilla JSON format.
 */
class JsonArtifact(private val src: JsonElement) : Artifact {
    override fun url(): String = src.getString("url")

    override fun path(): String = src.getString("path").ifBlank { src.getString("id") }

    override fun size(): ULong = src.gets("size")?.jsonPrimitive?.long?.toULong() ?: 0UL

    override fun checksum(): String = "sha1=${src.getString("sha1").ifBlank { return "" }}"
}

/**
 * An implementation of [Argument] based on vanilla JSON format.
 */
class JsonArgument(private val src: JsonElement) : Argument {
    override fun values(): List<String> =
        if (src is JsonObject) {
            src["value"]?.let {
                if (it is JsonArray) it.map { it.jsonPrimitive.content }
                else listOf(it.jsonPrimitive.content)
            } ?: emptyList()
        } else listOf(src.jsonPrimitive.content)

    override fun rules(): List<Rule> {
        if (src is JsonObject) {
            src.gets("rules")?.takeIf { it is JsonArray }?.jsonArray?.map { JsonRule(it) }?.let { return it }
        }
        return emptyList()
    }
}

/**
 * Links two profiles together with null safety. Creates a new [LinkedLaunchProfile] on demand.
 */
fun linkProfile(base: LaunchProfile?, head: LaunchProfile?): LaunchProfile =
    when {
        head == null && base == null -> throw IllegalArgumentException("Linking two null profiles")
        head == null -> base!!
        base == null -> head
        else -> {
            debug("Patched ${head.id()} -> ${base.id()}")
            LinkedLaunchProfile(base, head)
        }
    }

/**
 * An implementation of [Argument] based on vanilla JSON format.
 */
class LinkedLaunchProfile(private val base: LaunchProfile, private val head: LaunchProfile) : LaunchProfile {
    // Merge two nullable parts
    private fun <T> merge(b: T, h: T): T =
        if (h is String) { // Merge candidates must be of the same type
            h.ifBlank { b }
        } else {
            h ?: b
        }

    override fun id(): String = merge(base.id(), head.id())

    override fun version(): String = merge(head.version(), base.version()) // Version of the base profile wins

    override fun libraries(): List<Library> = head.libraries() + base.libraries()

    override fun inheritsFrom(): String = base.inheritsFrom()

    override fun jvmArguments(): List<Argument> = base.jvmArguments() + head.jvmArguments()

    override fun gameArguments(): List<Argument> = base.gameArguments() + head.gameArguments()

    override fun mainClass(): String = merge(base.mainClass(), head.mainClass())

    override fun assetId(): String = merge(base.assetId(), head.assetId())

    override fun assetIndexArtifact(): Artifact? = merge(base.assetIndexArtifact(), head.assetIndexArtifact())

    override fun loggingArtifact(): Artifact? = merge(base.loggingArtifact(), head.loggingArtifact())

    override fun jreComponent(): String = merge(base.jreComponent(), head.jreComponent())

    override fun jreVersion(): Int = head.jreVersion().takeIf { it > 0 } ?: base.jreVersion()

    override fun clientArtifact(): Artifact? = merge(base.clientArtifact(), head.clientArtifact())

    override fun clientMappingsArtifact(): Artifact? =
        merge(base.clientMappingsArtifact(), head.clientMappingsArtifact())

    override fun versionType(): String = merge(base.versionType(), head.versionType())
}