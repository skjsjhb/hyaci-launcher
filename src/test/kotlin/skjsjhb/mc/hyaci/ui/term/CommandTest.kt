package skjsjhb.mc.hyaci.ui.term

import kotlin.test.Test
import kotlin.test.assertEquals

class CommandTest {
    @Test
    fun `Command Parsing with Spaces`() {
        val s = """
            mkfs /path/to/spaced\ file   Test
        """.trimIndent()

        Command.of(s).run {
            assertEquals("mkfs", subject())
            assertEquals("/path/to/spaced file", unnamed(0))
            assertEquals("Test", unnamed(1))
        }
    }

    @Test
    fun `Command Parsing with Values`() {
        val s = """
            mkfs arg1=sp\ ace arg2\=not\ an\ argument
        """.trimIndent()

        Command.of(s).run {
            assertEquals("mkfs", subject())
            assertEquals("sp ace", named("arg1"))
            assertEquals("arg2=not an argument", unnamed(0))
        }
    }
}