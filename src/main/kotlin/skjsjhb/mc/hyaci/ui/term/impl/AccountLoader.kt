package skjsjhb.mc.hyaci.ui.term.impl

import skjsjhb.mc.hyaci.auth.Account
import skjsjhb.mc.hyaci.auth.AccountManager
import skjsjhb.mc.hyaci.ui.term.ArgumentAdapter
import kotlin.reflect.KType
import kotlin.reflect.full.createType

class AccountLoader : ArgumentAdapter<Account> {
    override fun get(src: String): Account = AccountManager.getAccounts().let { accounts ->
        if (accounts.size == 1) accounts.first() else accounts.find {
            it.username().lowercase().contains(src.lowercase()) || it.uuid().contains(src.lowercase())
        } ?: throw NoSuchElementException("No account named '$accounts' (or similar to)")
    }

    override fun target(): KType = Account::class.createType()
}