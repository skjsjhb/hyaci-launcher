package skjsjhb.mc.hyaci.ui.term

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import java.io.InputStreamReader

class TerminalUiProvider {
    private val inputReader = InputStreamReader(System.`in`)
    private val commandExecutor = CommandExecutor()

    fun launch() {
        showHint()

        while (true) {
            val cmd = promptNextCommand() ?: break
            if (cmd.subject().isBlank()) continue
            commandExecutor.dispatch(cmd)
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

    private fun promptNextCommand(): Command? {
        AnsiConsole.out().print(Ansi.ansi().fgDefault().a(">>> "))
        return inputReader.readCommand()
    }
}

