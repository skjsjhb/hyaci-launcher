package skjsjhb.mc.hyaci.auth

import skjsjhb.mc.hyaci.security.Keys
import skjsjhb.mc.hyaci.sys.openDBConnection
import skjsjhb.mc.hyaci.util.debug
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.sql.Blob
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Manages and stores account objects.
 */
object AccountManager {
    private val connection by lazy { openDBConnection() }

    private const val tableName = "ACCOUNTS"
    private const val blobColumnName = "BIN"
    private const val idColumnName = "UID"

    init {
        createAccountTable()
    }

    /**
     * Gets a list of all accounts present.
     *
     * Blob data of the accounts is decrypted using the system key before returned.
     */
    fun getAccounts(): Set<Account> = mutableSetOf<Account>().apply {
        connection.createStatement().use {
            it.executeQuery("SELECT $blobColumnName FROM $tableName").use {
                val key = Keys.exportSystemKey()
                while (it.next()) {
                    add(it.getBlob(1).decryptToAccount(key))
                }
                runCatching { key.destroy() }
            }
        }
    }

    /**
     * Adds or updates the given [Account] in the database.
     *
     * Accounts are indexed by their UUIDs. Therefore, two accounts cannot be distinguished if they share the same
     * UUID (which is quite unlikely). An account without or with blank UUID will not be stored, and an exception
     * will be thrown.
     *
     * Blob data of the account is encrypted using the system key before written.
     */
    fun putAccount(account: Account) {
        account.uuid().ifBlank { throw IllegalArgumentException("Cannot add an account without UUID") }
        val key = Keys.exportSystemKey()
        connection.prepareStatement("MERGE INTO $tableName ($idColumnName, $blobColumnName) VALUES (?, ?)")
            .apply {
                setString(1, account.uuid())
                setBlob(2, account.encryptToBlob(key))
            }.use { it.execute().also { debug("Merged account ${account.uuid()}") } }
        runCatching { key.destroy() }
    }

    /**
     * Removes an [Account] from the database.
     *
     * Fails silently if the given account does not provide a valid UUID.
     */
    fun removeAccount(account: Account) {
        account.uuid().ifBlank { return }
        connection.prepareStatement("DELETE FROM $tableName WHERE $idColumnName = ?")
            .apply {
                setString(1, account.uuid())
            }.use { it.execute().also { debug("Removed account ${account.uuid()}") } }
    }

    /**
     * Clear account records.
     */
    fun clearAccounts() {
        connection.createStatement().use { it.execute("DROP TABLE IF EXISTS $tableName") }
    }

    private fun createAccountTable() {
        connection.createStatement().use {
            it.execute("CREATE TABLE IF NOT EXISTS $tableName ($idColumnName VARCHAR NOT NULL PRIMARY KEY, $blobColumnName BLOB NOT NULL)")
        }
    }

    private fun Blob.decryptToAccount(key: SecretKey): Account =
        ObjectInputStream(
            CipherInputStream(
                binaryStream,
                Cipher.getInstance("AES").apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(key.encoded, "AES")) })
        ).use { it.readObject() as Account }

    private fun Account.encryptToBlob(key: SecretKey): Blob =
        connection.createBlob().also {
            ObjectOutputStream(
                CipherOutputStream(
                    it.setBinaryStream(1),
                    Cipher.getInstance("AES").apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.encoded, "AES")) }
                )
            ).use { it.writeObject(this) }
        }
}