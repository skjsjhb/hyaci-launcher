package skjsjhb.mc.hyaci.profile

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RulesTest {
    @Test
    fun `Matching JSON Rules`() {
        val src = """
            {
                "libraries": [
                    {
                        "rules": [
                            {
                                "action": "allow",
                                "os": {
                                    "name": "hyaci"
                                }
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val profile = Profile.load("foo") { src }
        val rule = profile.libraries().first().rules()
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
                    "os.name" to "hyaci", "os.arch" to "x128"
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