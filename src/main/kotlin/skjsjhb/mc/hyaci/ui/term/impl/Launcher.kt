package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.auth.Account
import skjsjhb.mc.hyaci.launch.Game
import skjsjhb.mc.hyaci.launch.LaunchPack
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import skjsjhb.mc.hyaci.ui.term.compose.WithAdapters
import skjsjhb.mc.hyaci.ui.term.requireConfirm
import skjsjhb.mc.hyaci.ui.term.tinfo
import skjsjhb.mc.hyaci.vfs.Vfs

class Launcher : CommandProcessor {
    @WithAdapters(VfsLoader::class, AccountLoader::class)
    @Usage(
        """
        launch <id> <fs> <account> [wait] - Launches the game.
            id - Profile ID.
            fs - VFS where the profile is on.
            account - Name or UUID of an account, or their substring long enough to identify one.
            wait - Whether to wait for the game to exit.
    """
    )
    fun launch(id: String, fs: Vfs, account: Account, wait: Boolean = true): Boolean {
        tinfo("Launch the game $id on VFS ${fs.name()}, with account ${account.username()} (${account.uuid()}, ${account::class.simpleName}).")
        requireConfirm("Is this correct?")

        val launchPack = LaunchPack(id, fs, createMinimumRuleSet(), account)
        Game(launchPack).run {
            start()
            if (wait) {
                join()
            }
        }
        return true
    }

    private fun createMinimumRuleSet(): Map<String, String> = mutableMapOf(
        "os.name" to Canonical.osName(),
        "os.version" to Canonical.osVersion(),
        "os.arch" to Canonical.osArch()
    )
}