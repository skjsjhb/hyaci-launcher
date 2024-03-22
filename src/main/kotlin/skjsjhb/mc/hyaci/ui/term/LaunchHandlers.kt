package skjsjhb.mc.hyaci.ui.term

import skjsjhb.mc.hyaci.auth.AccountManager
import skjsjhb.mc.hyaci.launch.Game
import skjsjhb.mc.hyaci.launch.LaunchPack
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.vfs.VfsManager

class LaunchCommandHandler : AbstractCommandHandler("launch") {
    override fun handle(command: Command): Boolean {
        val id = command.get("id", 0)
        val fsName = command.get("vfs", 1)
        val accountName = command.get("account", 2).lowercase()
        val fs = VfsManager.get(fsName)
        val account = AccountManager.findAccount(accountName)

        tinfo("Launch the game $id on VFS '$fsName', with account ${account.username()} (${account.uuid()}, ${account::class.simpleName}).")
        askConfirm("Is this correct?").let { if (!it) return false }

        val launchPack = LaunchPack(id, fs, createMinimumRuleSet(), account)
        Game(launchPack).run {
            start()
            join()
        }
        return true
    }

    private fun createMinimumRuleSet(): Map<String, String> = mutableMapOf(
        "os.name" to Canonical.osName(),
        "os.version" to Canonical.osVersion(),
        "os.arch" to Canonical.osArch()
    )
}