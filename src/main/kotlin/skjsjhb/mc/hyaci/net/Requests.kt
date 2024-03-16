package skjsjhb.mc.hyaci.net

import java.io.InputStreamReader
import java.net.URI

/**
 * Retrieves content as string from the given URL.
 */
fun retrieveString(url: String): String =
    URI(url).toURL().openStream().use {
        InputStreamReader(it).readText()
    }
