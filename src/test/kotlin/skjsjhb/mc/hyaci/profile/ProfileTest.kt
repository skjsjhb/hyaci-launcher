package skjsjhb.mc.hyaci.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileTest {
    @Test
    fun `Profile Linking`() {
        Profile.load("fabric-loader-0.15.6-1.20.4") {
            javaClass.getResource("/$it.json")?.readText() ?: ""
        }.apply {
            assertEquals("fabric-loader-0.15.6-1.20.4", id())
            assertEquals(96, libraries().size)
            assertEquals("1.20.4", version())
            assertTrue { inheritsFrom().isBlank() }
            assertEquals(17, jreVersion())
            assertEquals("12", assetId())
            assertEquals("net.fabricmc.loader.impl.launch.knot.KnotClient", mainClass())
        }
    }

    @Test
    fun `Legacy Profile Loading`() {
        Profile.load("1.12.2") { javaClass.getResource("/1.12.2.json")!!.readText() }
            .apply {
                assertTrue { gameArgs().isNotEmpty() }
                assertTrue { jvmArgs().isNotEmpty() }
            }
    }
}