package skjsjhb.mc.hyaci.vfs

import java.nio.file.Path

/**
 * Implements vanilla 1.6+ file resolution strategy.
 *
 * This class is open for the convenience of creating other FSes based on
 * the vanilla structure.
 *
 * @param root The root directory (i.e., game directory).
 */
open class VanillaFs(private val root: Path) : Vfs {
    override fun profile(id: String): Path = resolve("versions/$id/$id.json")

    override fun client(id: String): Path = resolve("versions/$id/$id.jar")

    // Do not store at $natives to avoid file overwriting
    override fun natives(id: String): Path = resolve("versions/$id/natives")

    override fun library(path: String): Path = resolve("libraries/$path")

    override fun assetRoot(): Path = resolve("assets")

    override fun asset(hash: String): Path = resolve("assets/objects/${hash.slice(0..1)}/$hash")

    override fun assetIndex(id: String): Path = resolve("assets/indexes/$id.json")

    override fun logConfig(id: String): Path = resolve(id)

    override fun gameDir(): Path = resolve(".")

    override fun resolve(rel: String): Path = root.resolve(rel).toAbsolutePath().normalize()
}