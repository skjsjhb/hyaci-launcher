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
        return s.ifBlank { named(name) }?.ifBlank { InteractionContext.requestInput(name) } ?: ""
    }

    /**
     * Gets the argument of the given name and/or index. If not provided, then use the default value.
     */
    fun get(name: String, index: Int = -1, def: String): String {
        val s = if (index >= 0) unnamed().getOrNull(index) ?: "" else ""
        return s.ifBlank { named(name) }?.ifBlank { def } ?: def
    }

    companion object {
        fun of(src: String): Command = StringCommand(src)
    }
}

private class StringCommand(private val src: String) : Command {
    private val subject: String
    private val unnamed: MutableList<String>
    private val named: MutableMap<String, String> = HashMap()

    init {
        var shouldEscape = false
        var hasValue = false
        val keyBuffer = StringBuilder()
        val valueBuffer = StringBuilder()
        LinkedList<String>().apply {
            // A trailing space is added for the convenience of handling the last token
            ("$src ").forEach { c ->
                if (shouldEscape) {
                    if (hasValue) {
                        valueBuffer.append(c)
                    } else {
                        keyBuffer.append(c)
                    }
                    shouldEscape = false
                } else {
                    when (c) {
                        ' ' -> {
                            // To handle delimiters of multiple spaces, we only break if the key buffer has data
                            // Value buffer can be empty
                            if (keyBuffer.isNotEmpty()) {
                                if (hasValue) {
                                    named[keyBuffer.toString()] = valueBuffer.toString()
                                } else {
                                    add(keyBuffer.toString())
                                }
                                keyBuffer.clear()
                                valueBuffer.clear()
                                hasValue = false
                            }
                        }

                        '\\' -> {
                            shouldEscape = true
                        }

                        '=' -> {
                            hasValue = true
                        }

                        else -> {
                            if (hasValue) {
                                valueBuffer.append(c)
                            } else {
                                keyBuffer.append(c)
                            }
                        }
                    }
                }
            }
        }.let {
            subject = it.poll() ?: ""
            unnamed = it // Use the rest elements
        }
    }

    override fun subject(): String = subject

    override fun unnamed(): List<String> = unnamed

    override fun unnamed(index: Int): String? {
        if (index < 0 || index >= unnamed.size) return null
        return unnamed[index]
    }

    override fun named(key: String): String? = named.getOrDefault(key, null)

    override fun toString(): String = src
}