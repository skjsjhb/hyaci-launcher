package skjsjhb.mc.hyaci.auth

import kotlinx.serialization.json.*
import skjsjhb.mc.hyaci.net.Requests
import skjsjhb.mc.hyaci.sys.Canonical
import skjsjhb.mc.hyaci.sys.dataPathOf
import skjsjhb.mc.hyaci.sys.forkClass
import skjsjhb.mc.hyaci.util.Sources
import skjsjhb.mc.hyaci.util.debug
import skjsjhb.mc.hyaci.util.getString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Serializable
import kotlin.io.path.exists

/**
 * Vanilla OAuth-based account.
 *
 * @param internalId An ID used to identify this account.
 */
class VanillaAccount(private val internalId: String) : Account, Serializable {
    private var oauthCode: String = ""
    private var oauthToken: String = ""
    private var refreshToken: String = ""
    private var xblToken: String = ""
    private var userHash: String = ""
    private var xstsToken: String = ""
    private var xuid: String = ""
    private var accessToken: String = ""
    private var uuid: String = ""
    private var playerName: String = ""

    private fun String.blankThenThrow(what: String): String =
        ifBlank { throw IllegalArgumentException("Invalid $what") }

    override fun update() {
        updateOAuthToken()
        updateTokens()
    }

    override fun username(): String = playerName

    override fun uuid(): String = uuid

    override fun token(): String = accessToken

    override fun xuid(): String = xuid

    override fun validate(): Boolean =
        runCatching {
            updateProfileContent()
            true
        }.getOrDefault(false)

    // Updates the OAuth access token
    private fun updateOAuthToken() {
        runCatching {
            if (refreshToken.isNotBlank()) {
                refreshOAuthToken() // Try refresh first
                return
            }
        }

        runCatching {
            if (oauthToken.isNotBlank()) {
                fetchOAuthToken() // The code here may be outdated
                return
            }
        }
        // Get the new code (no more chances to fail!)
        oauthCode.ifBlank { browserLogin() }
        fetchOAuthToken()
    }

    private fun fetchOAuthToken() {
        debug("Fetching OAuth token")
        tokenRequest("code", "authorization_code", oauthCode)
    }

    private fun refreshOAuthToken() {
        debug("Refreshing OAuth token")
        tokenRequest("refresh_token", "refresh_token", refreshToken)
    }

    private fun tokenRequest(grantTag: String, grantType: String, code: String) {
        Requests.postString(
            Sources.OAUTH_API.value,
            mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            "client_id=00000000402b5328&${grantTag}=${code}&grant_type=${grantType}" +
                    "&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf" +
                    "&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL"
        ).let { Json.parseToJsonElement(it) }.run {
            oauthToken = getString("access_token").blankThenThrow("OAuth access token")
            refreshToken = getString("refresh_token").blankThenThrow("OAuth refresh token")
        }
    }

    private fun updateXblToken() {
        debug("Token -> XBL")
        Requests.postJson(Sources.XBL_API.value, buildJsonObject {
            putJsonObject("Properties") {
                put("AuthMethod", "RPS")
                put("SiteName", "user.auth.xboxlive.com")
                put("RpsTicket", oauthToken)
            }
            put("RelyingParty", "http://auth.xboxlive.com")
            put("TokenType", "JWT")
        }).run {
            xblToken = getString("Token").blankThenThrow("XBL token")
            userHash = getString("DisplayClaims.xui.0.uhs").blankThenThrow("user hash")
        }
    }

    private fun updateXstsToken() {
        debug("XBL -> XSTS")
        Requests.postJson(Sources.XSTS_API.value, buildJsonObject {
            putJsonObject("Properties") {
                put("SandboxId", "RETAIL")
                putJsonArray("UserTokens") { add(xblToken) }
            }
            put("RelyingParty", "rp://api.minecraftservices.com/")
            put("TokenType", "JWT")
        }).run {
            xstsToken = getString("Token").blankThenThrow("XSTS token")
        }
    }

    private fun updateXuid() {
        debug("XBL -> XUID")
        Requests.postJson(Sources.XSTS_API.value, buildJsonObject {
            putJsonObject("Properties") {
                put("SandboxId", "RETAIL")
                putJsonArray("UserTokens") { add(xblToken) }
            }
            put("RelyingParty", "http://xboxlive.com")
            put("TokenType", "JWT")
        }).run {
            xuid = getString("DisplayClaims.xui.0.xid").blankThenThrow("XUID")
        }
    }

    private fun updateMojangToken() {
        debug("XSTS -> Mojang")
        Requests.postJson(Sources.MOJANG_LOGIN_API.value, buildJsonObject {
            put("identityToken", "XBL3.0 x=$userHash;$xstsToken")
        }).run {
            accessToken = getString("access_token").blankThenThrow("access token")
        }
    }

    private fun updateProfileContent() {
        debug("Mojang -> Profile")
        Requests.getString(
            Sources.MOJANG_PROFILE_API.value,
            mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $accessToken"
            )
        ).let { Json.parseToJsonElement(it) }.run {
            uuid = getString("id").blankThenThrow("UUID")
            playerName = getString("name").blankThenThrow("player name")
        }
    }

    // Perform requests after the oauth token has been retrieved
    private fun updateTokens() {
        updateXblToken()
        updateXstsToken()
        updateXuid()
        updateMojangToken()
        updateProfileContent()

        debug("Completed vanilla authentication")
    }

    // Opens a browser and blocks until user login
    private fun browserLogin() {
        val authProc = forkClass(
            "skjsjhb.mc.hyaci.auth.VanillaAccountHelper",
            listOf(internalId),
            helperJvmFlags()
        )

        // Use error stream to avoid color escapes
        val lines = BufferedReader(InputStreamReader(authProc.errorStream)).lines()
        for (l in lines) {
            if (l.startsWith("OAuth Code: ")) {
                oauthCode = l.replace("OAuth Code: ", "")
                break
            } else {
                println("Message from helper: $l")
            }
        }
        oauthCode.blankThenThrow("oauth code")
    }

    private fun helperJvmFlags(): List<String> = when (Canonical.osName()) {
        "osx" -> listOf(
            "-XstartOnFirstThread",
            "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
        )

        else -> emptyList()
    }

    companion object {
        private const val serialVersionUID = 1L

        /**
         * Checks whether the browser has been installed and ready for use.
         */
        fun isBrowserReady(): Boolean = dataPathOf("jcef-bundle").exists()
    }
}

