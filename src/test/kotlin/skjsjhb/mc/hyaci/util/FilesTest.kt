package skjsjhb.mc.hyaci.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FilesTest {
    @Test
    fun `Generate Checksum`() {
        assertEquals("1eba7caf09a39110ad2f542e3ed8700d1a69c6d3", checksumOf("LICENSE", "sha1"))
    }
}