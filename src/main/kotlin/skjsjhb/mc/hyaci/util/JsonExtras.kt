package skjsjhb.mc.hyaci.util

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Gets a key split by dots in the given [JsonElement].
 */
fun JsonElement.gets(id: String): JsonElement? {
    val names = id.split(".")
    var current: JsonElement = this
    for (k in names) {
        if (current !is JsonObject) return null
        current = current.jsonObject[k] ?: return null
    }
    return current
}

/**
 * Gets an element using [gets] and converts it to a [String].
 */
fun JsonElement.getString(id: String, def: String = ""): String =
    gets(id).let { if (it is JsonPrimitive && it.isString) it.content else def }