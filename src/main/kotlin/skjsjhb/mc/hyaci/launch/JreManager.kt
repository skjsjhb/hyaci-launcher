package skjsjhb.mc.hyaci.launch

import skjsjhb.mc.hyaci.sys.openDBConnection

object JreManager {
    private val connection by lazy {
        openDBConnection().apply {
            createStatement().use {
                it.execute("CREATE TABLE IF NOT EXISTS $tableName ($componentColumnName VARCHAR UNIQUE PRIMARY KEY NOT NULL, $binColumnName TEXT NOT NULL)")
            }
        }
    }

    private const val tableName = "JRE_LIST"
    private const val componentColumnName = "COMPONENT"
    private const val binColumnName = "BIN"

    /**
     * Gets the path to registered java binary path.
     */
    fun get(componentName: String): String {
        connection.prepareStatement("SELECT $binColumnName FROM $tableName WHERE $componentColumnName = ?").use {
            it.setString(1, componentName)
            it.executeQuery().use {
                if (!it.next()) throw NoSuchElementException("No JRE component $componentName")
                return it.getString(1)
            }
        }
    }

    /**
     * Puts a component with the corresponding binary path.
     */
    fun put(componentName: String, bin: String) {
        connection.prepareStatement("MERGE INTO $tableName ($componentColumnName, $binColumnName) VALUES (?, ?)")
            .use {
                it.setString(1, componentName)
                it.setString(2, bin)
                it.execute()
            }
    }
}