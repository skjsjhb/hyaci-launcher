package skjsjhb.mc.hyaci.container

import skjsjhb.mc.hyaci.sys.blobOf
import skjsjhb.mc.hyaci.sys.openDBConnection
import skjsjhb.mc.hyaci.sys.toObject

/**
 * Manages and store registered containers.
 */
object ContainerManager {
    private val connection = openDBConnection()

    private const val tableName = "CONTAINERS"
    private const val nameColumnName = "C_NAME"
    private const val objColumnName = "C_OBJ"

    init {
        connection.createStatement().use {
            it.execute("CREATE TABLE IF NOT EXISTS $tableName ($nameColumnName VARCHAR UNIQUE PRIMARY KEY NOT NULL, $objColumnName BLOB NOT NULL)")
        }
    }

    fun put(c: Container) {
        connection.prepareStatement("MERGE INTO $tableName ($nameColumnName, $objColumnName) VALUES ( ?, ? )").use {
            it.setString(1, c.name())
            it.setBlob(2, connection.blobOf(c))
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

    fun get(name: String): Container {
        connection.prepareStatement("SELECT $objColumnName FROM $tableName WHERE $nameColumnName = ?").use {
            it.setString(1, name)
            it.executeQuery().use {
                if (!it.next()) throw NoSuchElementException("No container named $name")
                return it.getBlob(1).toObject() as Container
            }
        }
    }

    fun remove(name: String) {
        connection.prepareStatement("DELETE FROM $tableName WHERE $nameColumnName = ?").use {
            it.setString(1, name)
            it.execute()
        }
    }

    fun getAll(): Set<Container> =
        connection.createStatement().use {
            it.executeQuery("SELECT $objColumnName FROM $tableName").use { rs ->
                mutableSetOf<Container>().apply {
                    while (rs.next()) {
                        add(rs.getBlob(1).toObject() as Container)
                    }
                }
            }
        }
}