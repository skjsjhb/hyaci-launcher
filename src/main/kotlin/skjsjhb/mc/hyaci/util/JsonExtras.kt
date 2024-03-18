package skjsjhb.mc.hyaci.util

import kotlinx.serialization.json.*

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

fun JsonElement.getBoolean(id: String, def: Boolean = false): Boolean =
    gets(id).let { if (it is JsonPrimitive) it.booleanOrNull ?: def else def }

fun JsonElement.getInt(id: String, def: Int = 0): Int =
    gets(id).let { if (it is JsonPrimitive) it.intOrNull ?: def else def }

fun JsonElement.getLong(id: String, def: Long = 0L): Long =
    gets(id).let { if (it is JsonPrimitive) it.longOrNull ?: def else def }

fun JsonElement.getDouble(id: String, def: Double = 0.0): Double =
    gets(id).let { if (it is JsonPrimitive) it.doubleOrNull ?: def else def }

fun JsonElement.getFloat(id: String, def: Float = 0f): Float =
    gets(id).let { if (it is JsonPrimitive) it.floatOrNull ?: def else def }

fun JsonElement.getArray(id: String): JsonArray? =
    gets(id).let { if (it is JsonArray) it else null }

fun JsonElement.getObject(id: String): JsonObject? =
    gets(id).let { if (it is JsonObject) it else null }