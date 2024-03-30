package skjsjhb.mc.hyaci.launch

import skjsjhb.mc.hyaci.sys.Canonical
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalTest {
    @Test
    fun `Retrieve OS Name`() {
        System.setProperty("os.name", "macOS Ultimate")
        assertEquals("osx", Canonical.osName())

        System.setProperty("os.name", "Hyaci Linux") // This is imaginary
        assertEquals("linux", Canonical.osName())
    }
}