package skjsjhb.mc.hyaci.auth

import skjsjhb.mc.hyaci.util.warn
import java.io.Serializable
import java.util.*

/**
 * An account variant used when playing demo mode.
 *
 * @param name Player name.
 */
class DemoAccount(private val name: String) : Account, Serializable {

    init {
        warn("You're warned that demo accounts SHOULD NOT be used to crack the game.")
    }

    override fun update() {}

    override fun username(): String = name

    override fun uuid(): String =
        UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray()).toString().replace("-", "")

    override fun token(): String = "SUPER_SECRET_TOKEN_DO_NOT_USE_OR_YOU_WILL_BE_FIRED"

    override fun xuid(): String = "SUPER_SECRET_XUID_DO_NOT_USE_OR_YOU_WILL_BE_FIRED"

    override fun validate(): Boolean = true

    companion object {
        private const val serialVersionUID = 1L
    }
}