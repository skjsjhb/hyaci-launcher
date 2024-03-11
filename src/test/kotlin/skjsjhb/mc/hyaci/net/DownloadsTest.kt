package skjsjhb.mc.hyaci.net

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadsTest {
    @Test
    fun `Download File`() {
        val a = artifactOf(
            "https://piston-data.mojang.com/v1/objects/099bf3a8ad10d4a4ca8acc3f7347458ed7db16ec/client.jar",
            "client.jar",
            25122499UL,
            "sha1=099bf3a8ad10d4a4ca8acc3f7347458ed7db16ec"
        )
        val task = DownloadTask(a)
        assertTrue { task.resolve().get() }
        assertTrue { task.finished() }
        assertEquals(DownloadTaskStatus.DONE, task.status)
        assertTrue { task.progress() == 1.0 }
        assertTrue { task.speed() > 0 }
        assertTrue { Files.exists(Path.of("client.jar")) }

        val task2 = DownloadTask(a)
        task2.resolve().get()
        assertEquals(DownloadTaskStatus.SKIPPED, task2.status)
        Files.delete(Path.of("client.jar"))
    }
}