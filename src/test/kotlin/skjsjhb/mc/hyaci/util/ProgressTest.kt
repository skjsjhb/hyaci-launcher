package skjsjhb.mc.hyaci.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressTest {
    @Test
    fun `Progress Counting`() {
        val src = listOf(1, 2, 3)
        var progress = 0.0
        src.withProgress { _, p -> progress = p }.find { it == 2 }
        assertEquals(2.0 / 3, progress)
    }
}