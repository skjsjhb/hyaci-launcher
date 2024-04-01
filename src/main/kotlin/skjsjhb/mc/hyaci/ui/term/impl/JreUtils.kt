package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.install.JreInstaller
import skjsjhb.mc.hyaci.launch.JreManager
import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.InteractionContext
import skjsjhb.mc.hyaci.ui.term.compose.CommandName
import skjsjhb.mc.hyaci.ui.term.compose.Usage

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
        InteractionContext.run {
            JreManager.put(name, bin)
            info("Java component list altered.")
        }
    }

    @CommandName("install.java")
    @Usage(
        """
        install.java <name> - Install official Java component globally.
            name - Name of the component.
    """
    )
    fun install(name: String) {
        InteractionContext.run {
            info("Install Java component: $name")
            requestConfirm("Is this correct?")
            JreInstaller(name).run {
                setProgressHandler { status, progress -> printProgress(status, progress) }
                install()
            }
            info("Completed installation of $name.")
        }
    }
}