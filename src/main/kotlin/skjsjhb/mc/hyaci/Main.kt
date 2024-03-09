@file:JvmName("Main") // Preserve classname

package skjsjhb.mc.hyaci

import skjsjhb.mc.hyaci.util.info

/**
 * Main entry of the application.
 */
fun main() {
    showLicense()
}

private fun showLicense() {
    """
    Hyaci Launcher 1.0
    "Another place, another way."
    -----
    Copyright (C) 2024 Ted "skjsjhb" Gao
    This program comes with ABSOLUTELY NO WARRANTY.
    This is free software, and you are welcome to redistribute it under certain conditions.
    See the about page for details.
    -----
    """.trimIndent().split("\n").forEach { info(it) }
}