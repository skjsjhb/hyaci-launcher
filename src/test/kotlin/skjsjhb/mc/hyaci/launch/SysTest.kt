package skjsjhb.mc.hyaci.launch

import kotlin.test.Test
import kotlin.test.assertEquals

class SysTest {
    @Test
    fun `Retrieve OS Name`() {
        System.setProperty("os.name", "macOS Ultimate")
        assertEquals("osx", canonicalOSName())

        System.setProperty("os.name", "Hyaci Linux") // This is imaginary
        assertEquals("linux", canonicalOSName())
    }
}