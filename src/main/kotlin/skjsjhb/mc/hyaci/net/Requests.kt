package skjsjhb.mc.hyaci.net

import java.io.InputStreamReader
import java.net.URI

object Requests {
    /**
     * Retrieves content as string.
     */
    fun string(url: String): String = URI(url).toURL().openStream().use {
        InputStreamReader(it).readText()
    }
}