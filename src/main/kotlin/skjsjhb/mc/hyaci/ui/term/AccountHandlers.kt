package skjsjhb.mc.hyaci.ui.term

import skjsjhb.mc.hyaci.auth.Account
import skjsjhb.mc.hyaci.auth.AccountManager
import skjsjhb.mc.hyaci.auth.DemoAccount
import skjsjhb.mc.hyaci.auth.VanillaAccount
import java.util.*

abstract class MakeAccountCommandHandler(vararg suffixes: String) :
    AbstractCommandHandler("mkac", aliases = suffixes.map { "mkac.$it" }.toTypedArray()) {

    abstract fun createAccount(command: Command): Account?

    final override fun handle(command: Command): Boolean {
        if (command.subject() == "mkac") {
            terror("Please specify the type of account to create.")
            return false
        }
        createAccount(command)?.let {
            AccountManager.putAccount(it)
            tinfo("Welcome, ${it.username()}! (${it.uuid()}, ${it::class.simpleName})")
        }
        return true
    }
}

class MakeDemoAccountCommandHandler : MakeAccountCommandHandler("demo") {
    override fun createAccount(command: Command): Account {
        val playerName = command.get("name", 0)
        return DemoAccount(playerName)
    }
}

class MakeVanillaAccountCommandHandler : MakeAccountCommandHandler("vanilla", "ms", "microsoft") {
    override fun createAccount(command: Command): Account? {
        tinfo("A browser will pop up for you to add your premium account.")
        tinfo("Please complete the login in the browser, and we'll handle the rest.")
        askConfirm("Continue?").let { if (!it) return null }
        if (!VanillaAccount.isBrowserReady()) {
            tinfo("We need to download some files for the login. This may take a couple of minutes.")
            askConfirm("Continue?")
        }
        return VanillaAccount(UUID.randomUUID().toString()).apply { update() }
    }
}

class ListAccountCommandHandler : AbstractCommandHandler("lsac") {
    override fun handle(command: Command): Boolean {
        AccountManager.getAccounts().forEach {
            tinfo("- ${it.username()} (${it.uuid()}, ${it::class.simpleName})")
        }
        return true
    }
}

class RemoveAccountCommandHandler : AbstractCommandHandler("rmac") {
    override fun handle(command: Command): Boolean {
        val accountName = command.get("name", 0)
        val account = AccountManager.findAccount(accountName)
        tinfo("Will delete account ${account.username()} (${account.uuid()}, ${account::class.simpleName}).")
        twarn("This is irrevocable!")
        askConfirm("Continue removing?").let { if (!it) return false }
        AccountManager.removeAccount(account)
        tinfo("Removed specified account.")
        return true
    }
}

fun AccountManager.findAccount(name: String): Account =
    getAccounts().let { accounts ->
        if (accounts.size == 1) accounts.first() else accounts.find {
            it.username().lowercase().contains(name) || it.uuid().contains(name)
        } ?: throw NoSuchElementException("No account named '$accounts' (or similar to)")
    }
