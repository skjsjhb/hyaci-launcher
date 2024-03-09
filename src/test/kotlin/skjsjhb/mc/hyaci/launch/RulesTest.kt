package skjsjhb.mc.hyaci.launch

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RulesTest {
    @Test
    fun `Matching JSON Rules`() {
        val json = """
            {
                "action": "allow",
                "os": {
                    "name": "hyaci"
                }
            }
        """.trimIndent()
        val rule = JsonRule(Json.parseToJsonElement(json))
        assertTrue {
            rule.accepts(
                mapOf(
                    "os.name" to "hyaci"
                )
            )
        }

        assertTrue {
            rule.accepts(
                mapOf(
                    "os.name" to "hyaci",
                    "os.arch" to "x128"
                )
            )
        }

        assertFalse {
            rule.accepts(
                mapOf(
                    "os.name" to "wowdins",
                )
            )
        }
    }
}