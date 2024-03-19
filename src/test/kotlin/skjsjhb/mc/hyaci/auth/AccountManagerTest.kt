package skjsjhb.mc.hyaci.auth

import kotlin.test.Test
import kotlin.test.assertTrue

class AccountManagerTest {
    @Test
    fun `Test Insert Account`() {
        val ac = DemoAccount("Test")
        AccountManager.putAccount(ac)
        assertTrue { AccountManager.getAccounts().map { it.uuid() }.contains(ac.uuid()) }
    }
}