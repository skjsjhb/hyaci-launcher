package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.container.Container
import skjsjhb.mc.hyaci.install.VanillaInstaller
import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.InteractionContext
import skjsjhb.mc.hyaci.ui.term.compose.CommandName
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import skjsjhb.mc.hyaci.ui.term.compose.WithAdapters

@Suppress("unused")
class Installation : CommandProcessor {
    @WithAdapters(ContainerLoader::class)
    @CommandName("install.game")
    @Usage(
        """
        install.game <id> <c> - Install vanilla game.
            id - Profile ID.
            c - Container to install the profile on.
    """
    )
    fun installGame(id: String, c: Container) {
        InteractionContext.run {
            info("Install vanilla game $id on container ${c.name()}.")
            requestConfirm("Is this correct?")
            VanillaInstaller(id, c).run {
                setProgressHandler { status, progress -> printProgress(status, progress) }
                install()
            }
            info("Completed installation of $id.")
        }
    }
}