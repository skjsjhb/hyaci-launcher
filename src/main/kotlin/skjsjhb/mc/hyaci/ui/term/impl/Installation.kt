package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.install.VanillaInstaller
import skjsjhb.mc.hyaci.ui.term.*
import skjsjhb.mc.hyaci.vfs.Vfs

@Suppress("unused")
class Installation : CommandProcessor {
    @WithAdapters(VfsLoader::class)
    @CommandName("install.game")
    @Usage(
        """
        install.game <id> <fs> - Install vanilla game.
            id - Profile ID.
            fs - VFS to install the profile on.
    """
    )
    fun installGame(id: String, fs: Vfs) {
        tinfo("Install vanilla game $id on VFS ${fs.name()}.")
        tinfo("(This may take some minutes and many logs will appear)")
        requireConfirm("Is this correct?")
        VanillaInstaller(id, fs).install()
        tinfo("Completed installation of $id.")
    }
}