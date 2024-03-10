package skjsjhb.mc.hyaci.net

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

class DownloadsTest {
    @Test
    fun `Download File`() {
        val a = immediateArtifactOf(
            "https://piston-data.mojang.com/v1/objects/099bf3a8ad10d4a4ca8acc3f7347458ed7db16ec/client.jar",
            "client.jar"
        )
        val task = DownloadTask(a)
        assertTrue { task.resolve().get() }
        assertTrue { task.finished() }
        assertTrue { task.progress() == 1.0 }
        assertTrue { task.speed.get() > 0 }
        assertTrue { Files.exists(Path.of("client.jar")) }
        Files.delete(Path.of("client.jar"))
    }
}