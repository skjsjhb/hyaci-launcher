package skjsjhb.mc.hyaci.vfs

import skjsjhb.mc.hyaci.sys.blobOf
import skjsjhb.mc.hyaci.sys.openDBConnection
import skjsjhb.mc.hyaci.sys.toObject

/**
 * Manages and store registered VFS instances.
 */
object VfsManager {
    private val connection = openDBConnection()

    private const val tableName = "VFS_REGISTRY"
    private const val nameColumnName = "V_NAME"
    private const val objColumnName = "V_OBJ"

    init {
        connection.createStatement().use {
            it.execute("CREATE TABLE IF NOT EXISTS $tableName ($nameColumnName VARCHAR UNIQUE PRIMARY KEY NOT NULL, $objColumnName BLOB NOT NULL)")
        }
    }

    fun put(fs: Vfs) {
        connection.prepareStatement("MERGE INTO $tableName ($nameColumnName, $objColumnName) VALUES ( ?, ? )").use {
            it.setString(1, fs.name())
            it.setBlob(2, connection.blobOf(fs))
            it.execute()
        }
    }

    fun exists(name: String): Boolean {
        connection.prepareStatement("SELECT COUNT(*) FROM $tableName WHERE $nameColumnName = ?").use {
            it.setString(1, name)
            it.executeQuery().use {
                if (!it.next()) return false
                return it.getInt(1) > 0
            }
        }
    }

    fun get(name: String): Vfs {
        connection.prepareStatement("SELECT $objColumnName FROM $tableName WHERE $nameColumnName = ?").use {
            it.setString(1, name)
            it.executeQuery().use {
                if (!it.next()) throw NoSuchElementException("No VFS named $name")
                return it.getBlob(1).toObject() as Vfs
            }
        }
    }

    fun remove(name: String) {
        connection.prepareStatement("DELETE FROM $tableName WHERE $nameColumnName = ?").use {
            it.setString(1, name)
            it.execute()
        }
    }

    fun getAll(): Set<Vfs> =
        connection.createStatement().use {
            it.executeQuery("SELECT $objColumnName FROM $tableName").use { rs ->
                mutableSetOf<Vfs>().apply {
                    while (rs.next()) {
                        add(rs.getBlob(1).toObject() as Vfs)
                    }
                }
            }
        }
}