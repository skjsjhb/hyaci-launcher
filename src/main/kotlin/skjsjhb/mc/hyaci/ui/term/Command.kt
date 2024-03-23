package skjsjhb.mc.hyaci.ui.term

import java.util.*

interface Command {
    /**
     * Gets the subject of the command.
     */
    fun subject(): String

    /**
     * Gets all unnamed arguments.
     */
    fun unnamed(): List<String>

    /**
     * Gets an unnamed argument at given index.
     */
    fun unnamed(index: Int): String?

    /**
     * Gets named arguments.
     */
    fun named(key: String): String?

    /**
     * Gets the argument of the given name and/or index. If not provided, then ask the user.
     */
    fun get(name: String, index: Int = -1): String {
        val s = if (index >= 0) unnamed().getOrNull(index) ?: "" else ""
        return s.ifBlank { named(name) }?.ifBlank { askMore(name) } ?: ""
    }

    /**
     * Gets the argument of the given name and/or index. If not provided, then use the default value.
     */
    fun get(name: String, index: Int = -1, def: String): String {
        val s = if (index >= 0) unnamed().getOrNull(index) ?: "" else ""
        return s.ifBlank { named(name) }?.ifBlank { def } ?: def
    }

    companion object CommandUtils {
        fun of(src: String): Command = StringCommand(src)
    }
}

private class StringCommand(src: String) : Command {
    private val subject: String
    private val unnamed: MutableList<String> = ArrayList()
    private val named: MutableMap<String, String> = HashMap()

    init {
        Scanner(src).apply {
            subject = next().lowercase()
            while (hasNext()) {
                var opt = next()
                if (opt.startsWith("\"")) {
                    useDelimiter("\"")
                    opt += next()
                    skip("\"")
                }
                useDelimiter(" ")
                if (opt.startsWith("\"")) {
                    opt = opt.substring(1)
                }
                if (opt.contains("=")) {
                    opt.split("=", limit = 2).let {
                        named.put(it[0], it[1])
                    }
                } else {
                    unnamed.add(opt)
                }
            }
        }
    }

    override fun subject(): String = subject

    override fun unnamed(): List<String> = unnamed

    override fun unnamed(index: Int): String? {
        if (index < 0 || index >= unnamed.size) return null
        return unnamed[index]
    }

    override fun named(key: String): String? = named.getOrDefault(key, null)
}