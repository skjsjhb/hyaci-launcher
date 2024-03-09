package skjsjhb.mc.hyaci.util

import skjsjhb.mc.hyaci.sys.forkClass
import kotlin.test.Test
import kotlin.test.assertEquals

class ForkTest {
    @Test
    fun `Fork Class`() {
        val secret = "SUPER_SECRET_VALUE"
        val output = forkClass("skjsjhb.mc.hyaci.util.ForkClient", listOf(secret)).inputReader().readLine()
        assertEquals(secret, output)
    }
}