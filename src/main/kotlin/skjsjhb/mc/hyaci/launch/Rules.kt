package skjsjhb.mc.hyaci.launch

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import skjsjhb.mc.hyaci.util.getObject
import skjsjhb.mc.hyaci.util.getString

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
infix fun List<Rule>.accepts(rv: Map<String, String>): Boolean =
    if (isEmpty()) true // When rules are empty, then fallback to pass
    else asReversed().stream().filter { it accepts rv }.map { it.action() }.findFirst().orElse(false)

/**
 * An implementation of [Rule] based on vanilla JSON format.
 */
class JsonRule(private val src: JsonElement) : Rule {
    override fun accepts(rv: Map<String, String>): Boolean =
        mutableListOf<Pair<String, String>>().apply {
            // Collect properties with qualified name
            listOf("os", "features").forEach { tag ->
                src.getObject(tag)?.let {
                    addAll(it.map { (k, v) -> Pair("$tag.$k", v.jsonPrimitive.content) })
                }
            }
        }.all { (k, e) -> rv[k]?.matches(e.toRegex()) ?: false }

    override fun action(): Boolean = src.getString("action") == "allow"
}
