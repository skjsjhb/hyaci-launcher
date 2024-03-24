package skjsjhb.mc.hyaci.ui.term

import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CommandReaderTest {

    @Test
    fun `Read Command`() {
        val content = """
            cmd1 arg1 arg2
            cmd2 line con\
            junction
            cmd3 slash\ es
        """.trimIndent()
        CommandReader(StringReader(content)).run {
            assertNotNull(readCommand()).run {
                assertEquals("arg2", unnamed(1))
            }
            assertNotNull(readCommand()).run {
                assertEquals("conjunction", unnamed(1))
            }
            assertNotNull(readCommand()).run {
                assertEquals("slash es", unnamed(0))
            }
            assertNull(readCommand())
        }
    }
}