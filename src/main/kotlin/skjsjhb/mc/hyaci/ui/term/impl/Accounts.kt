package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.auth.Account
import skjsjhb.mc.hyaci.auth.AccountManager
import skjsjhb.mc.hyaci.auth.DemoAccount
import skjsjhb.mc.hyaci.auth.VanillaAccount
import skjsjhb.mc.hyaci.ui.term.CommandProcessor
import skjsjhb.mc.hyaci.ui.term.InteractionContext
import skjsjhb.mc.hyaci.ui.term.compose.CommandName
import skjsjhb.mc.hyaci.ui.term.compose.Usage
import skjsjhb.mc.hyaci.ui.term.compose.WithAdapters
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
        InteractionContext.run {
            info("A browser will pop up for you to add your premium account.")
            info("Please complete the login in the browser, and we'll handle the rest.")
            requestConfirm()
            if (!VanillaAccount.isBrowserReady()) {
                info("We need to download some files for the login. This may take a couple of minutes.")
                requestConfirm()
            }
            addAccount(VanillaAccount(UUID.randomUUID().toString()).apply { update() })
        }
    }

    @WithAdapters(AccountLoader::class)
    @CommandName("updac")
    @Usage(
        """
        updac <account> - Update an account.
            account - Name or UUID of an account, or their substring long enough to identify one.
    """
    )
    fun updateAccount(account: Account) {
        InteractionContext.run {
            account.update()
            info("Account updated.")
        }
    }

    @WithAdapters(AccountLoader::class)
    @CommandName("seac")
    @Usage(
        """
        seac <account> - View account credentials.
            account - Name or UUID of an account, or their substring long enough to identify one. 
    """
    )
    fun credentials(account: Account) {
        InteractionContext.run {
            warn("WAIT!")
            warn("There is usually no reason to expose credentials of an account.")
            warn("Be aware, sharing credentials brings SEVERE risks!")
            requestConfirm("I know what I'm doing!")
            info("Username: ${account.username()}")
            info("UUID: ${account.uuid()}")
            info("Access token: ${account.token()}")
            warn("==== DO NOT SHARE THE CONTENT ABOVE TO ANYONE ====")
        }
    }

    @CommandName("lsac")
    @Usage("lsac - List all accounts.")
    fun listAll() {
        InteractionContext.run {
            AccountManager.getAccounts().forEach {
                info("- ${it.username()} (${it.uuid()}, ${it::class.simpleName})")
            }
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
        InteractionContext.run {
            info("Will delete account ${account.toReadableString()}).")
            warn("This is irrevocable!")
            requestConfirm("Absolutely sure?")
            AccountManager.removeAccount(account)
            info("Removed that account.")
        }
    }

    private fun addAccount(account: Account) {
        InteractionContext.run {
            AccountManager.putAccount(account)
            info("Welcome, ${account.toReadableString()}!")
        }
    }
}

private fun Account.toReadableString(): String = "${username()} (${uuid()}, ${this::class.simpleName})"