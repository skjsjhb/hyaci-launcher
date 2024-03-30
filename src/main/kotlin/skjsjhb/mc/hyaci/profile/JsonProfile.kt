package skjsjhb.mc.hyaci.profile

import kotlinx.serialization.json.*
import skjsjhb.mc.hyaci.net.Artifact
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.util.*

/**
 * An implementation of [Profile] backed by a JSON document of vanilla format.
 */
class JsonProfile(private val src: JsonElement) : Profile {
    override fun id(): String = src.getString("id")

    // This implementation does not give the correct result when multi-inheritance happens. This can be resolved during
    // the linking process, where the version eventually "falls into" a Mojang qualified version.
    override fun version(): String =
        src.getArray("patches")  // Compatibility with some merged profiles
            ?.find { it.getString("id") == "game" }
            ?.getString("version")
            .let {
                if (it.isNullOrBlank()) src.getString("inheritsFrom", id()) // Official qualified version names
                else it
            }

    override fun libraries(): List<Library> =
        src.getArray("libraries").orEmpty().map { JsonLibrary(it) }

    override fun inheritsFrom(): String = src.getString("inheritsFrom")

    private fun genArguments(name: String): List<Argument> =
        src.getArray("arguments.$name").orEmpty().map { JsonArgument(it) }

    private fun loggingArgument(): Argument? {
        return src.getString("logging.client.argument").takeIf { it.isNotBlank() }?.let {
            object : Argument {
                override fun values(): List<String> = listOf(it)
                override fun rules(): List<Rule> = emptyList()
            }
        }
    }

    override fun jvmArgs(): List<Argument> =
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

    override fun gameArgs(): List<Argument> =
        src.getString("minecraftArguments").takeIf { it.isNotBlank() }?.let {
            it.split(" ").map {
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

    override fun jreVersion(): Int = src.getInt("javaVersion.majorVersion")

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
private class JsonLibrary(private val src: JsonElement) : Library {
    override fun name(): String = src.getString("name")

    override fun rules(): List<Rule> = src.getArray("rules").orEmpty().map { JsonRule(it) }

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
private class JsonArtifact(private val src: JsonElement) : Artifact {
    override fun url(): String = src.getString("url")

    override fun path(): String = src.getString("path").ifBlank { src.getString("id") }

    override fun size(): ULong = src.getLong("size").toULong()

    override fun checksum(): String = "sha1=${src.getString("sha1").ifBlank { return "" }}"
}

/**
 * An implementation of [Argument] based on vanilla JSON format.
 */
private class JsonArgument(private val src: JsonElement) : Argument {
    override fun values(): List<String> =
        if (src is JsonObject) {
            src["value"]?.let {
                if (it is JsonArray) it.map { it.jsonPrimitive.content }
                else listOf(it.jsonPrimitive.content)
            } ?: emptyList()
        } else listOf(src.jsonPrimitive.content)

    override fun rules(): List<Rule> {
        if (src is JsonObject) {
            src.getArray("rules")?.map { JsonRule(it) }?.let { return it }
        }
        return emptyList()
    }
}