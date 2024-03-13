package skjsjhb.mc.hyaci.net

import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
        task.resolveOrThrow()
        assertTrue { task.finished() }
        assertEquals(DownloadTaskStatus.DONE, task.status)
        assertEquals(1.0, task.progress().asPercentage())
        assertTrue { task.speed() > 0 }
        assertTrue { Files.exists(Path.of("client.jar")) }

        val task2 = DownloadTask(a)
        task2.resolveOrThrow()
        assertEquals(DownloadTaskStatus.SKIPPED, task2.status)
        Files.delete(Path.of("client.jar"))
    }

    @Test
    fun `Download Multiple Files`() {
        mutableSetOf(
            "https://piston-meta.mojang.com/v1/packages/39222ffc70b35f3193b39eb0268bb446cfc6b7e6/24w10a.json",
            "https://piston-meta.mojang.com/v1/packages/9467a0a2d5257e166a617ec314e6dbfcf8eb5ef8/24w09a.json",
            "https://piston-meta.mojang.com/v1/packages/121163bc6624810b57362543bf3606d0747c61a7/24w07a.json",
            "https://piston-meta.mojang.com/v1/packages/532610f1e06dabec430702ed04b82a045b7091a4/24w06a.json"
        ).map {
            artifactOf(it, Path.of(URI(it).toURL().path).fileName.toString())
        }.toSet().let {
            val grp = DownloadGroup(it)
            assertTrue { grp.resolve() }
            listOf("24w10a.json", "24w09a.json", "24w07a.json", "24w06a.json").forEach {
                Files.delete(Path.of(it))
            }
        }
    }

    @Test
    fun `Download Failure`() {
        mutableSetOf(
            "https://piston-meta.mojang.com/v1/packages/unicorn.json", // Not exist
            "https://piston-meta.mojang.com/v1/packages/39222ffc70b35f3193b39eb0268bb446cfc6b7e6/24w10a.json",
            "https://piston-meta.mojang.com/v1/packages/9467a0a2d5257e166a617ec314e6dbfcf8eb5ef8/24w09a.json",
            "https://piston-meta.mojang.com/v1/packages/121163bc6624810b57362543bf3606d0747c61a7/24w07a.json",
            "https://piston-meta.mojang.com/v1/packages/532610f1e06dabec430702ed04b82a045b7091a4/24w06a.json"
        ).map {
            artifactOf(it, Path.of(URI(it).toURL().path).fileName.toString())
        }.toSet().let {
            val grp = DownloadGroup(it)

            // This IOException is concealed deeply...
            val err = assertThrows<ExecutionException> { grp.resolveOrThrow() } // ExecutorService wraps the exception
            val rt1 = assertIs<RuntimeException>(err.cause) // ForkAndJoinPool rebuilds the exception
            val rt2 = assertIs<RuntimeException>(rt1.cause) // ForkAndJoinTask wraps it in RuntimeException
            assertIs<IOException>(rt2.cause)

            listOf("24w10a.json", "24w09a.json", "24w07a.json", "24w06a.json").forEach {
                assertTrue { Files.exists(Path.of(it)) }
                Files.delete(Path.of(it))
            }
        }
    }

    @Test
    fun `Interrupt Downloading`() {
        val task = DownloadTask(
            artifactOf(
                "https://piston-data.mojang.com/v1/objects/099bf3a8ad10d4a4ca8acc3f7347458ed7db16ec/client.jar",
                "client.jar"
            )
        )

        val flag = AtomicBoolean(false)
        Thread {
            Thread.sleep(100) // Hopefully the file is still downloading
            while (!flag.get()) task.cancel()
        }.start()
        try {
            task.resolve()
        } catch (_: Exception) {
        } finally {
            flag.set(true)
        }

        Files.delete(Path.of("client.jar"))

        assertEquals(DownloadTaskStatus.CANCELED, task.status)
    }
}