package skjsjhb.mc.hyaci.profile

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
    fun accepts(rv: Map<String, String>): Boolean

    /**
     * The action to be performed when all criteria are met.
     * `true` for allow and `false` for disallow.
     */
    fun action(): Boolean
}

/**
 * Represents profile elements whose availability is determined by a set of rules.
 */
interface RuleManaged {
    /**
     * Rules of this library.
     */
    fun rules(): List<Rule>
}

/**
 * Checks if the given value map satisfies the defined rules.
 */
fun List<Rule>?.accepts(rv: Map<String, String>): Boolean =
    if (isNullOrEmpty()) true // When rules are empty, then fallback to pass
    else asReversed().stream().filter { it.accepts(rv) }.map { it.action() }.findFirst().orElse(false)

/**
 * Convenient method for checking rules of given [RuleManaged] element.
 */
fun RuleManaged.accepts(rv: Map<String, String>): Boolean = rules().accepts(rv)

/**
 * Convenient method for filtering a collection of [RuleManaged] elements.
 */
fun <E : RuleManaged> Collection<E>.filterRules(rv: Map<String, String>): List<E> =
    filter { it.accepts(rv) }

/**
 * An implementation of [Rule] based on vanilla JSON format.
 */
class JsonRule(private val src: JsonElement) : Rule {
    override fun accepts(rv: Map<String, String>): Boolean =
        listOf("os", "features")
            .all { tag ->
                src.getObject(tag)?.all { (k, v) ->
                    rv["$tag.$k"]?.matches(v.jsonPrimitive.content.toRegex()) ?: false
                } ?: true
            }

    override fun action(): Boolean = src.getString("action") == "allow"
}

