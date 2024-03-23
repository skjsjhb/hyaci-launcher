package skjsjhb.mc.hyaci.ui.term

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole

class TerminalUiProvider {
    fun launch() {
        showHint()

        while (true) {
            val cmd = nextCommand() ?: break
            if (cmd.isBlank()) continue
            CommandExecutor.dispatch(Command.of(cmd))
        }
    }

    private fun showHint() {
        Ansi.ansi().fgBrightCyan().a(
            """
            [Hyaci Launcher Commandline Interface]
            Type 'help' for more information.
        """.trimIndent()
        ).let { AnsiConsole.out().println(it) }

        Ansi.ansi().fgBrightYellow().a(
            """
            CAUTION: COMMANDS CANNOT BE UNDONE!
            Priceless the data. Check before entering.
        """.trimIndent()
        ).let { AnsiConsole.out().println(it) }
    }

    private fun nextCommand(): String? {
        AnsiConsole.out().print(Ansi.ansi().fgDefault().a(">>> "))
        return StringBuilder().apply {
            while (true) {
                val s = readlnOrNull() ?: return null
                if (!s.endsWith("\\")) {
                    append(s)
                    break
                }
                append(s.substring(0, s.length - 1))
            }
        }.toString()
    }
}

