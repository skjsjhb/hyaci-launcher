package skjsjhb.mc.hyaci.ui.term

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import java.util.*

/**
 * A class for handling UI interactions.
 */
object InteractionContext {
    private val inputScanner = Scanner(System.`in`)

    /**
     * Request the user to give a positive response towards given condition.
     *
     * @param what The message to display to the user.
     * @param optional Whether the confirmation is optional. Rejected mandatory confirmation will lead to an exception.
     */
    fun requestConfirm(what: String = "Continue?", optional: Boolean = false): Boolean {
        printColorfulLine { fgDefault().a("? $what (yes/no): ") }
        while (true) {
            val a = inputScanner.nextLine().lowercase()
            if (a == "yes") return true
            if (a == "no") {
                if (optional) return false
                else throw IllegalStateException("Confirmation rejected")
            }
            printColorfulLine { fgDefault().a("? Please type 'yes' or 'no': ") }
        }
    }

    /**
     * Prompt user to input a value for the given argument name.
     *
     * This method can be useful when a mandatory argument is missing for the given command, without forcing the user
     * to type the command again.
     */
    fun requestInput(what: String): String {
        AnsiConsole.out().print(Ansi.ansi().fgDefault().a("? Value for '$what': "))
        val i = inputScanner.nextLine()
        if (i.isNotBlank()) return i
        throw IllegalArgumentException("Value '$what' is mandatory, but not provided")
    }

    fun warn(what: String) {
        printColorfulLine { fgBrightYellow().a("! $what") }
    }

    fun error(what: String) {
        printColorfulLine { fgBrightRed().a("! $what") }
    }

    fun info(what: String) {
        printColorfulLine { fgBrightCyan().a(what) }
    }

    private fun printColorfulLine(what: Ansi.() -> Ansi) {
        AnsiConsole.out().println(Ansi.ansi().what())
    }

    private fun printColorful(what: Ansi.() -> Ansi) {
        AnsiConsole.out().print(Ansi.ansi().what())
    }
}