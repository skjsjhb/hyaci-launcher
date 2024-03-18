package skjsjhb.mc.hyaci.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

private const val bufferSize = 512

/**
 * Calculates the checksum of the given file.
 *
 * The resulted hash string is in lowercase, encoded as HEX.
 */
fun checksum(f: String, algo: String): String =
    MessageDigest.getInstance(algo).apply {
        FileInputStream(f).use {
            val bytes = ByteArray(bufferSize)
            while (true) {
                val bytesRead = it.read(bytes)
                if (bytesRead == -1) {
                    break
                }
                update(bytes, 0, bytesRead)
            }
        }
    }.digest().let {
        StringBuilder().apply {
            it.forEach { append(String.format("%02x", it)) }
        }.toString().lowercase()
    }

/**
 * Unpacks a zip file.
 */
fun unzip(f: String, out: String) {
    ZipInputStream(FileInputStream(f)).use { zip ->
        var ent = zip.nextEntry
        while (ent != null) {
            File("$out/${ent.name}").let {
                it.mkdirs()
                FileOutputStream(it).use { zip.transferTo(it) }
            }
            ent = zip.nextEntry
        }
    }
}
