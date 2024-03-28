package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.auth.Account
import skjsjhb.mc.hyaci.auth.DemoAccount
import skjsjhb.mc.hyaci.launch.Game
import skjsjhb.mc.hyaci.launch.LaunchPack
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import skjsjhb.mc.hyaci.ui.term.compose.WithAdapters
import skjsjhb.mc.hyaci.ui.term.requireConfirm
import skjsjhb.mc.hyaci.ui.term.tinfo
import skjsjhb.mc.hyaci.ui.term.twarn
import skjsjhb.mc.hyaci.vfs.Vfs

class Launcher : CommandProcessor {
    @WithAdapters(VfsLoader::class, AccountLoader::class)
    @Usage(
        """
        launch <id> <fs> <account> [java] [wait] - Launches the game.
            id - Profile ID.
            fs - VFS where the profile is on.
            account - Name or UUID of an account, or their substring long enough to identify one.
            java - Name of the Java runtime component to use. If not provided, a value specified in the profile will be used.
            wait - Whether to wait for the game to exit. Default to true.
    """
    )
    fun launch(id: String, fs: Vfs, account: Account, java: String = "", wait: Boolean = true): Boolean {
        val isDemoAccount = account is DemoAccount
        if (isDemoAccount) {
            twarn("Demo account detected, the game will be launched in demo mode.")
        }

        tinfo("Launch the game $id on VFS ${fs.name()}, with account ${account.username()} (${account.uuid()}, ${account::class.simpleName}).")
        requireConfirm("Is this correct?")

        val ruleValues = createMinimumRuleSet() + mapOf(
            "features.is_demo_user" to isDemoAccount.toString()
        )

        val launchPack = LaunchPack(id, fs, ruleValues, account, java)
        Game(launchPack).run {
            start()
            if (wait) join()
        }
        return true
    }

    private fun createMinimumRuleSet(): Map<String, String> = mapOf(
        "os.name" to Canonical.osName(),
        "os.version" to Canonical.osVersion(),
        "os.arch" to Canonical.osArch()
    )
}