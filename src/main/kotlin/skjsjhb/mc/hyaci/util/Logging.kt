package skjsjhb.mc.hyaci.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("SameParameterValue")
private fun getCaller(offset: Long = 1) =
    StackWalker.getInstance().walk { it.skip(offset).findFirst().get() }.className.split("$")[0]

private fun getLogger(): Logger = LoggerFactory.getLogger(getCaller(4))

/**
 * Prints log of INFO level.
 */
fun info(s: String, t: Throwable? = null) {
    getLogger().run { if (t == null) info(s) else info(s, t) }
}

/**
 * Prints log of TRACE level.
 */
fun trace(s: String, t: Throwable? = null) {
    getLogger().run { if (t == null) trace(s) else trace(s, t) }
}

/**
 * Prints log of DEBUG level.
 */
fun debug(s: String, t: Throwable? = null) {
    getLogger().run { if (t == null) debug(s) else debug(s, t) }
}

/**
 * Prints log of WARN level.
 */
fun warn(s: String, t: Throwable? = null) {
    getLogger().run { if (t == null) warn(s) else warn(s, t) }
}

/**
 * Prints log of ERROR level.
 */
fun err(s: String, t: Throwable? = null) {
    getLogger().run { if (t == null) error(s) else error(s, t) }
}