package skjsjhb.mc.hyaci.ui.term

import java.io.Reader

/**
 * An extension to add the ability of reading a command from the given [Reader].
 *
 * This method blocks until a full line of command has been extracted.
 * Returns `null` when the host [Reader] reaches its end.
 */
fun Reader.readCommand(): Command? = StringBuilder().run {
    var connectLine = false
    var eos = false
    while (true) {
        when (val c = read()) {
            -1 -> {
                eos = true
                break
            }

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
    if (eos && isEmpty()) null else Command.of(toString())
}