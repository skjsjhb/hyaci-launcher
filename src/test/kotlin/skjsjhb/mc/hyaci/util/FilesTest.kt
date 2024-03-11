package skjsjhb.mc.hyaci.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FilesTest {
    @Test
    fun `Generate Checksum`() {
        assertEquals(
            "251364b90b8f139c16eb5d5ce376dfa697cba6cd",
            checksumOf("gradle/wrapper/gradle-wrapper.jar", "sha1")
        )
    }
}