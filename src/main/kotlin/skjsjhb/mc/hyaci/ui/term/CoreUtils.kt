package skjsjhb.mc.hyaci.ui.term

import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import skjsjhb.mc.hyaci.sys.Canonical
import kotlin.system.exitProcess

class AboutCommandHandler : AbstractCommandHandler("about", "version") {
    override fun handle(command: Command): Boolean {
        tinfo("${Canonical.appName()} ${Canonical.appVersion()}, with commandline interface")
        return true
    }
}

class ExitCommandHandler : AbstractCommandHandler("exit", "quit") {
    override fun handle(command: Command): Boolean {
        tinfo("OK I'm exiting.")

        // Reset color
        AnsiConsole.out().print(Ansi.ansi().fgDefault().bgDefault().a("\n"))

        exitProcess(0)
    }
}


