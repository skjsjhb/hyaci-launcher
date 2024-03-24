package skjsjhb.mc.hyaci.ui.term

import java.io.Reader

/**
 * A [Reader] which reads [Command] from the specified parent reader.
 */
class CommandReader(private val base: Reader) : Reader() {
    override fun read(cbuf: CharArray, off: Int, len: Int): Int = base.read(cbuf, off, len)

    override fun close() {
        base.close()
    }

    /**
     * Reads the next command.
     *
     * Returns the next read command, or null if the end of the reader has reached.
     * This method blocks until the next command is available.
     */
    fun readCommand(): Command? =
        StringBuilder().apply {
            var connectLine = false
            while (true) {
                when (val c = base.read()) {
                    -1 -> break
                    '\\'.code -> {
                        if (connectLine) append('\\') // The previous one is not a connector
                        connectLine = true
                    }

                    '\n'.code, '\r'.code -> {
                        if (!connectLine) break
                        connectLine = false
                    }

                    else -> {
                        if (connectLine) append('\\') // That is not a connector
                        append(c.toChar())
                        connectLine = false
                    }
                }
            }
        }.toString().let {
            Command.of(it.ifBlank { return null })
        }
}