package skjsjhb.mc.hyaci.container

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.file.Path

/**
 * Implements vanilla 1.6+ file resolution strategy.
 *
 * This class is open for the convenience of creating other FSes based on
 * the vanilla structure.
 *
 * @param name The name of the container.
 * @param root The root directory (i.e., game directory).
 */
open class VanillaContainer(private var name: String, private var root: Path) : Container, Serializable {
    override fun name(): String = name

    override fun profile(id: String): Path = resolve("versions/$id/$id.json")

    override fun client(id: String): Path = resolve("versions/$id/$id.jar")

    // Do not store at $natives to avoid file overwriting
    override fun natives(id: String): Path = resolve("versions/$id/natives")

    override fun library(path: String): Path = resolve("libraries/$path")

    override fun assetRoot(): Path = resolve("assets")

    override fun assetRootLegacy(): Path = resolve("assets/virtual/legacy")

    override fun assetRootMapToResources(): Path = resolve("resources")

    override fun asset(hash: String): Path = resolve("assets/objects/${hash.slice(0..1)}/$hash")

    override fun assetLegacy(fileName: String): Path = resolve("assets/virtual/legacy/$fileName")

    override fun assetMapToResources(fileName: String): Path = resolve("resources/$fileName")

    override fun assetIndex(id: String): Path = resolve("assets/indexes/$id.json")

    override fun logConfig(id: String): Path = resolve(id)

    override fun gameDir(): Path = resolve(".")

    override fun resolve(rel: String): Path = root.resolve(rel).toAbsolutePath().normalize()

    private fun writeObject(o: ObjectOutputStream) {
        o.writeObject(name)
        o.writeObject(root.toString())
    }

    private fun readObject(i: ObjectInputStream) {
        name = i.readObject() as String
        root = Path.of(i.readObject() as String)
    }

    companion object {
        private const val serialVersionUID = 2L
    }
}