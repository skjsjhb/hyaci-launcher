package skjsjhb.mc.hyaci.sys

import skjsjhb.mc.hyaci.util.debug
import skjsjhb.mc.hyaci.util.info
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Path
import java.sql.Blob
import java.sql.Connection
import java.sql.DriverManager
import java.util.stream.Stream

/**
 * Gets the absolute path to a file under the data path.
 */
fun dataPathOf(rel: String): Path = dataPathRoot.resolve(rel).toAbsolutePath().normalize()

/**
 * Gets a new connection to the main database.
 */
fun openDBConnection(): Connection =
    DriverManager.getConnection("jdbc:h2:${dataPathOf(dbName)};TRACE_LEVEL_FILE=0;DB_CLOSE_DELAY=-1")
        .also { debug("Opened connection to local database") }

private const val dbName = "hyaci"

private const val dataDirName = "Hyaci Launcher/astral" // Avoid installation conflicting

private val dataPathRoot: Path by lazy {
    (System.getProperty("hyaci.data.path")?.let {
        Path.of(it)
    } ?: when (Canonical.osName()) {
        "windows" ->
            Stream.of("LOCALAPPDATA", "APPDATA")
                .map { System.getenv(it) }
                .filter { it != null }
                .map { Path.of(it) }
                .findFirst()
                .orElse(Path.of(System.getProperty("user.home"), "Documents"))
                .resolve(dataDirName)

        "osx" ->
            Path.of(System.getProperty("user.home"), "Library/Application Support/$dataDirName")

        else -> Path.of(System.getProperty("user.home"), ".local/share/$dataDirName")
    }).toAbsolutePath().normalize().also { info("Data path set to $it") }
}

/**
 * Create a new blob, and write an object into it.
 */
fun Connection.blobOf(obj: Any): Blob =
    createBlob().apply {
        ObjectOutputStream(setBinaryStream(1)).use { it.writeObject(obj) }
    }

/**
 * Reads an object from the given [Blob].
 */
fun Blob.toObject(): Any =
    ObjectInputStream(binaryStream).use { it.readObject() }
