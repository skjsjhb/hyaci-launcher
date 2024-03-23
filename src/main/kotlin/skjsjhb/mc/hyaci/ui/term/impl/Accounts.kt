package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.auth.Account
import skjsjhb.mc.hyaci.auth.AccountManager
import skjsjhb.mc.hyaci.auth.DemoAccount
import skjsjhb.mc.hyaci.auth.VanillaAccount
import skjsjhb.mc.hyaci.ui.term.*
import java.util.*

@Suppress("unused")
class Accounts : CommandProcessor {
    @CommandName("mkac.demo")
    @Usage(
        """
        mkac.demo <playerName> - Creates a demo account.
            playerName - The player name.
    """
    )
    fun createDemo(playerName: String) {
        addAccount(DemoAccount(playerName))
    }

    @CommandName("mkac.vanilla")
    @Usage("mkac.vanilla - Add a premium account.")
    fun createVanilla() {
        tinfo("A browser will pop up for you to add your premium account.")
        tinfo("Please complete the login in the browser, and we'll handle the rest.")
        requireConfirm("Continue?")
        if (!VanillaAccount.isBrowserReady()) {
            tinfo("We need to download some files for the login. This may take a couple of minutes.")
            askConfirm("Continue?")
        }
        addAccount(VanillaAccount(UUID.randomUUID().toString()).apply { update() })
    }

    @CommandName("lsac")
    @Usage("lsac - List all accounts.")
    fun listAll() {
        AccountManager.getAccounts().forEach {
            tinfo("- ${it.username()} (${it.uuid()}, ${it::class.simpleName})")
        }
    }

    @WithAdapters(AccountLoader::class)
    @CommandName("rmac")
    @Usage(
        """
        rmac <account> - Remove an account.
            account - Name or UUID of an account, or their substring long enough to identify one.
    """
    )
    fun remove(account: Account) {
        tinfo("Will delete account ${account.toReadableString()}).")
        twarn("This is irrevocable!")
        requireConfirm("Continue removing?")
        AccountManager.removeAccount(account)
        tinfo("Removed that account.")
    }

    private fun addAccount(account: Account) {
        AccountManager.putAccount(account)
        tinfo("Welcome, ${account.toReadableString()}!")
    }
}

private fun Account.toReadableString(): String = "${username()} (${uuid()}, ${this::class.simpleName})"