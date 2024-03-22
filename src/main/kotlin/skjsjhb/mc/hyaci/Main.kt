@file:JvmName("Main") // Preserve classname

package skjsjhb.mc.hyaci

import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.ui.term.TerminalUiProvider
import skjsjhb.mc.hyaci.util.err
import skjsjhb.mc.hyaci.util.info

/**
 * Main entry of the application.
 */
fun main() {
    showLicense()

    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        err("Uncaught exception in thread ${t.name}", e)
    }

    TerminalUiProvider().launch()
}

private fun showLicense() {
    """
    ${Canonical.appName()} ${Canonical.appVersion()}
    -----
    Copyright (C) 2024 Ted "skjsjhb" Gao
    This program comes with ABSOLUTELY NO WARRANTY.
    This is free software, and you are welcome to redistribute it under certain conditions.
    See the about page for details.
    -----
    """.trimIndent().split("\n").forEach { info(it) }
}