package skjsjhb.mc.hyaci.launch

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Represents rules in the profile.
 */
interface Rule {
    /**
     * Checks whether this rule accepts the given value map.
     *
     * The value map is a set of properties (usually OS information and feature names) which are used
     */
    infix fun accepts(rv: Map<String, String>): Boolean

    /**
     * The action to be performed when all criteria are met.
     * `true` for allow and `false` for disallow.
     */
    fun action(): Boolean
}

/**
 * Checks if the given value map satisfies the defined rules.
 */
infix fun List<Rule>.accepts(rv: Map<String, String>): Boolean {
    if (this.isEmpty()) return true // When rules are empty, then fallback to pass
    var allow = false
    forEach {
        if (it accepts rv) allow = it.action()
    }
    return allow
}

/**
 * An implementation of [Rule] based on vanilla JSON format.
 */
class JsonRule(private val src: JsonElement) : Rule {
    override fun accepts(rv: Map<String, String>): Boolean {
        val criteria = HashMap<String, String>()

        // Collect properties with qualified name
        src.jsonObject["os"]?.jsonObject?.forEach { (k, v) -> criteria["os.$k"] = v.jsonPrimitive.content }
        src.jsonObject["features"]?.jsonObject?.forEach { (k, v) -> criteria["features.$k"] = v.jsonPrimitive.content }

        return criteria.all { (k, e) -> rv[k]?.matches(e.toRegex()) ?: false }
    }

    override fun action(): Boolean =
        src.jsonObject["action"]?.jsonPrimitive?.content == "allow"
}
