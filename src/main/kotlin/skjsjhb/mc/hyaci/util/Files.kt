package skjsjhb.mc.hyaci.util

import java.io.FileInputStream
import java.security.MessageDigest

private const val bufferSize = 512

/**
 * Calculates the checksum of the given file.
 *
 * The resulted hash string is in lowercase, encoded as HEX.
 */
fun checksumOf(f: String, algo: String): String =
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


