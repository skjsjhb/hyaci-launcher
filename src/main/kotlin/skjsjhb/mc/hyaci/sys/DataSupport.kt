package skjsjhb.mc.hyaci.sys

import skjsjhb.mc.hyaci.util.debug
import skjsjhb.mc.hyaci.util.info
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.stream.Stream
import kotlin.jvm.optionals.getOrElse

/**
 * Gets the absolute path to a file under the data path.
 */
fun dataPathOf(rel: String): Path = dataPathRoot.resolve(rel).toAbsolutePath().normalize()

/**
 * Gets a new connection to the main database.
 */
fun dbConnection(): Connection =
    DriverManager.getConnection("jdbc:h2:${dataPathOf(dbName)};TRACE_LEVEL_FILE=0;DB_CLOSE_DELAY=-1")
        .also { debug("Opened connection to local database") }

private const val dbName = "hyaci"

private const val dataDirName = "Hyaci Launcher"

private val dataPathRoot: Path =
    run {
        System.getProperty("hyaci.dataPath")?.let {
            Path.of(it)
        } ?: when (canonicalOSName()) {
            "windows" ->
                Stream.of("LOCALAPPDATA", "APPDATA")
                    .map { System.getenv(it) }
                    .filter { it != null }
                    .map { Path.of(it) }
                    .findFirst()
                    .getOrElse { Path.of(System.getProperty("user.home"), "Documents") }
                    .resolve(dataDirName)

            "osx" ->
                Path.of(System.getProperty("user.home"), "Library/Application Support/$dataDirName")

            else -> Path.of(System.getProperty("user.home"), ".local/share/$dataDirName")
        }
    }.toAbsolutePath().normalize().also { info("Data path set to $it") }




