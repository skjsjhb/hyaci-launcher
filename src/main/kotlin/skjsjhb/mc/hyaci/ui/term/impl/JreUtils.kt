package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.install.JreInstaller
import skjsjhb.mc.hyaci.launch.JreManager
import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.compose.CommandName
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import skjsjhb.mc.hyaci.ui.term.requireConfirm
import skjsjhb.mc.hyaci.ui.term.tinfo

@Suppress("unused")
class JreUtils : CommandProcessor {
    @CommandName("jreg")
    @Usage(
        """
        jreg <name> <bin> - Register custom Java component.
            name - Name of the component to add.
            bin - Path to the main executable.
    """
    )
    fun register(name: String, bin: String) {
        JreManager.put(name, bin)
        tinfo("Java component list altered.")
    }

    @CommandName("install.java")
    @Usage(
        """
        install.java <name> - Install official Java component globally.
            name - Name of the component.
    """
    )
    fun install(name: String) {
        tinfo("Install Java component: $name")
        requireConfirm("Is this correct?")
        JreInstaller(name).install()
        tinfo("Completed installation of $name.")
    }
}