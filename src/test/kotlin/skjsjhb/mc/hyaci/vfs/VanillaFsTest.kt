package skjsjhb.mc.hyaci.vfs

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class VanillaFsTest {
    @Test
    fun `VanillaFS Path Resolution`() {
        VanillaFs("test", Path.of("./hyaci")).run {
            assertEquals(
                Path.of("./hyaci/versions/1.20.4/1.20.4.json").toAbsolutePath().normalize(),
                profile("1.20.4")
            )
            assertEquals(
                Path.of("./hyaci/libraries/skjsjhb/mc/hyaci/1.0/hyaci-1.0.jar").toAbsolutePath().normalize(),
                library("skjsjhb/mc/hyaci/1.0/hyaci-1.0.jar")
            )
            assertEquals(
                Path.of("./hyaci/assets/objects/00/00aa").toAbsolutePath().normalize(),
                asset("00aa")
            )
            assertEquals(
                Path.of("./hyaci").toAbsolutePath().normalize(),
                gameDir()
            )
        }
    }
}