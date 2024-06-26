package skjsjhb.mc.hyaci.sys

import skjsjhb.mc.hyaci.util.warn

/**
 * Handles options of the application.
 */
object Options {
    private const val tableName = "OPTIONS"
    private const val keyColumnName = "KEI"
    private const val valueColumnName = "VAL"

    private val connection by lazy { openDBConnection() }

    init {
        createOptionsTable()
    }

    /**
     * Gets a string with default value.
     *
     * The key is first taken from the system properties, with a prefix `hyaci.options.`.
     * If not specified, then it queries the database and retrieves the corresponding value.
     */
    fun getString(key: String, def: String = ""): String =
        System.getProperty("hyaci.options.$key")
            ?: runCatching {
                connection.prepareStatement("SELECT $valueColumnName FROM $tableName WHERE $keyColumnName = ?")
                    .apply { setString(1, key) }
                    .use { it.executeQuery().takeIf { it.next() }?.getString(1) }
            }.onFailure {
                // This is only executed when SQL exceptions are thrown (not for empty keys)
                warn("Exception when reading option $key", it)
            }.getOrNull() ?: def

    /**
     * Gets an int with default value.
     */
    fun getInt(key: String, def: Int = 0): Int = getString(key).toIntOrNull() ?: def

    /**
     * Gets a double with default value.
     */
    fun getDouble(key: String, def: Double = 0.0): Double = getString(key).toDoubleOrNull() ?: def

    /**
     * Gets a boolean with default value.
     */
    fun getBoolean(key: String, def: Boolean = false): Boolean = getString(key).toBooleanStrictOrNull() ?: def

    /**
     * Puts an arbitrary object with default value. A `null` value will erase the key.
     */
    fun put(key: String, value: Any?) {
        if (value == null)
            connection
                .prepareStatement("DELETE FROM $tableName WHERE $keyColumnName = ?")
                .apply { setString(1, key) }
                .use {
                    it.execute()
                }
        else
            connection
                .prepareStatement("MERGE INTO $tableName ($keyColumnName, $valueColumnName) VALUES (?, ?)")
                .apply {
                    setString(1, key)
                    setString(2, value.toString())
                }.use {
                    it.execute()
                }
    }

    private fun createOptionsTable() {
        connection.createStatement().use {
            it.executeUpdate("CREATE TABLE IF NOT EXISTS $tableName ($keyColumnName VARCHAR PRIMARY KEY NOT NULL, $valueColumnName TEXT NOT NULL)")
        }
    }
}


