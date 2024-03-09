package skjsjhb.mc.hyaci.net

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

class DownloadsTest {
    @Test
    fun `Download File`() {
        val a = immediateArtifactOf(
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json",
            "version_manifest_v2.json"
        )
        DownloadTask(a).resolve().get()
        assertTrue { Files.exists(Path.of("version_manifest_v2.json")) }
        Files.delete(Path.of("version_manifest_v2.json"))
    }
}