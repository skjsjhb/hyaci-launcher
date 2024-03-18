package skjsjhb.mc.hyaci.auth

/**
 * Main player account interface.
 */
interface Account {
    /**
     * Updates account credentials.
     */
    fun update()

    /**
     * Gets the username.
     */
    fun username(): String

    /**
     * Gets the UUID.
     */
    fun uuid(): String

    /**
     * Gets the access token.
     */
    fun token(): String

    /**
     * Gets the XUID.
     */
    fun xuid(): String
}