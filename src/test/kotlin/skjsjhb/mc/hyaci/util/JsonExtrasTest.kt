package skjsjhb.mc.hyaci.util

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonExtrasTest {
    @Test
    fun `JSON Get`() {
        val json = """
            {
                "id": "aaa",
                "arr": [{ "bbb": "ccc" }],
                "obj": {
                    "astral": 1
                }
            }
        """.trimIndent()

        Json.parseToJsonElement(json).run {
            assertEquals("aaa", getString("id"))
            assertEquals("ccc", getString("arr.0.bbb"))
            assertEquals(1, getInt("obj.astral"))
        }
    }
}