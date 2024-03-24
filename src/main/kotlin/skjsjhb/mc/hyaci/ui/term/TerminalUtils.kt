package skjsjhb.mc.hyaci.ui.term

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import java.util.*

fun printa(what: Ansi.() -> Ansi) {
    val ansi = Ansi.ansi()
    AnsiConsole.out().print(ansi.what())
}

fun printaln(what: Ansi.() -> Ansi) {
    val ansi = Ansi.ansi()
    AnsiConsole.out().println(ansi.what())
}

fun twarn(what: String) {
    printaln { fgBrightYellow().a("! $what") }
}

fun terror(what: String) {
    printaln { fgBrightRed().a("! $what") }
}

fun tinfo(what: String) {
    printaln { fgBrightCyan().a(what) }
}

private val inputScanner = Scanner(System.`in`)

/**
 * Prompt user to input a value for the given argument name.
 *
 * This method can be useful when a mandatory argument is missing for the given command, without forcing the user
 * to type the command again.
 */
fun askMore(what: String): String {
    AnsiConsole.out().print(Ansi.ansi().fgDefault().a("? Enter a value for '$what': "))
    val i = inputScanner.nextLine()
    if (i.isNotBlank()) return i
    throw IllegalArgumentException("Value '$what' is mandatory, but not provided")
}

/**
 * Requests the user to confirm the given message.
 */
fun askConfirm(what: String): Boolean {
    printa { fgDefault().a("? $what (yes/no): ") }
    while (true) {
        val a = inputScanner.nextLine().lowercase()
        if (a == "yes") return true
        if (a == "no") return false
        printa { fgDefault().a("? Please enter 'yes' or 'no': ") }
    }
}

/**
 * Requests the user to confirm the given message. If the user did not confirm, then an exception is thrown.
 */
fun requireConfirm(what: String) {
    askConfirm(what).let { if (!it) throw IllegalStateException("Canceled") }
}
