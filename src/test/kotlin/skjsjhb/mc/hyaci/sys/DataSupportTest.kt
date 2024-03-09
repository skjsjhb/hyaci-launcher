package skjsjhb.mc.hyaci.sys

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class DataSupportTest {
    @Test
    fun `Get Data Root`() {
        System.setProperty("os.name", "Hyaci Linux")
        System.setProperty("user.home", "~")
        assertEquals(
            Path.of("~/.local/share/Hyaci Launcher").toAbsolutePath().normalize(),
            dataPathOf(".").toAbsolutePath().normalize()
        )
    }
}