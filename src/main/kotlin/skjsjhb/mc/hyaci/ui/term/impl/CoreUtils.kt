package skjsjhb.mc.hyaci.ui.term.impl

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.sys.Options
import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.InteractionContext
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import kotlin.system.exitProcess

@Suppress("unused")
class CoreUtils : CommandProcessor {
    @Usage("about - Display launcher version.")
    fun about() {
        InteractionContext.run {
            info("${Canonical.appName()} ${Canonical.appVersion()}")
            info("This launcher has super cow powers.")
        }
    }

    @Usage("exit - Exit launcher.")
    fun exit() {
        InteractionContext.run {
            info("OK I'm exiting.")
            AnsiConsole.out().print(Ansi.ansi().fgDefault().bgDefault().a("\n")) // Reset color
            exitProcess(0)
        }
    }

    @Usage(
        """
        opt <key> [value] - Set or get option item.
            key - Option key.
            value - When provided, the value will be set for the given key. Else the value of the key will be printed.
    """
    )
    fun opt(key: String, value: String = "") {
        InteractionContext.run {
            if (value.isNotBlank()) {
                Options.put(key, value)
                info("Option item altered.")
            } else {
                info("$key = ${Options.getString(key)}")
            }
        }
    }
}
