package skjsjhb.mc.hyaci.util

import java.io.RandomAccessFile
import java.math.BigInteger
import java.nio.channels.FileChannel
import java.security.MessageDigest

/**
 * Calculates the checksum of the given file.
 *
 * The resulted hash string is in lowercase, encoded as HEX.
 */
fun checksumOf(f: String, algo: String): String =
    RandomAccessFile(f, "r").use {
        it.channel.map(FileChannel.MapMode.READ_ONLY, 0, it.channel.size()).let {
            MessageDigest.getInstance(algo).apply { update(it) }.digest()
        }
    }.let { BigInteger(1, it).toString(16) }.lowercase()