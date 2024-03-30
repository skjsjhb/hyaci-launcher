package skjsjhb.mc.hyaci.util

import lzma.sdk.lzma.Decoder
import lzma.streams.LzmaInputStream
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
    }.digest().joinToString("") { String.format("%02x", it) }.lowercase()

/**
 * Unpacks a zip file.
 */
fun unzip(f: String, out: String) {
    ZipInputStream(FileInputStream(f)).use { zip ->
        var ent = zip.nextEntry
        while (ent != null) {
            if (!ent.isDirectory) {
                File("$out/${ent.name}").let {
                    it.parentFile.mkdirs()
                    FileOutputStream(it).use { zip.transferTo(it) }
                }
            }
            ent = zip.nextEntry
        }
    }
}

/**
 * Decompresses a lzma file.
 *
 * New directories are not automatically created.
 */
fun unlzma(f: String, out: String) {
    FileInputStream(f).use { fi ->
        LzmaInputStream(fi, Decoder()).use { input ->
            FileOutputStream(out).use { output ->
                input.transferTo(output)
            }
        }
    }
}

