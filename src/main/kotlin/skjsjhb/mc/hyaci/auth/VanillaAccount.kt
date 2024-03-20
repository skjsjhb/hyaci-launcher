package skjsjhb.mc.hyaci.auth

import kotlinx.serialization.json.*
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import skjsjhb.mc.hyaci.net.Requests
import skjsjhb.mc.hyaci.sys.dataPathOf
import skjsjhb.mc.hyaci.util.Sources
import skjsjhb.mc.hyaci.util.debug
import skjsjhb.mc.hyaci.util.getString
import skjsjhb.mc.hyaci.util.info
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.Serializable
import java.net.URI
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame
import javax.swing.WindowConstants

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
        val app = CefAppBuilder().run {
            setInstallDir(dataPathOf("jcef-bundle").toFile())
            setProgressHandler { state, percent ->
                debug("Setting up CEF browser: $state" + if (percent > 0) " ($percent%)" else "")
            }

            val cacheId = UUID.nameUUIDFromBytes(internalId.toByteArray()).toString()

            cefSettings.root_cache_path = dataPathOf("jcef-cache/ms-login/$cacheId").toString()
            cefSettings.cache_path = cefSettings.root_cache_path
            cefSettings.locale = Locale.getDefault().toLanguageTag()
            cefSettings.windowless_rendering_enabled = false
            cefSettings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_DISABLE

            build()
        }

        val client = app.createClient()
        val browser = client.createBrowser(Sources.OAUTH_WEB.value, false, false)
        val frame = JFrame("Login")
        val latch = CountDownLatch(1)
        val loginCompletePrefix = "https://login.live.com/oauth20_desktop.srf"

        client.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadStart(browser: CefBrowser, frame0: CefFrame, transition0: CefRequest.TransitionType) {
                if (browser.url.startsWith(loginCompletePrefix)) {
                    oauthCode = URI(browser.url).toURL().query
                        .split("&")
                        .associate {
                            it.split("=").toPair()
                        }["code"] ?: ""
                    debug("Code retrieved, closing browser window")
                    latch.countDown()
                }
            }
        })

        frame.add(browser.uiComponent)
        frame.pack()
        frame.size = Toolkit.getDefaultToolkit().screenSize.let {
            Dimension((it.width * 0.6).toInt(), (it.height * 0.6).toInt())
        }
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) {
                latch.countDown()
            }
        })

        info("Waiting for user login")
        latch.await()
        frame.isVisible = false
        frame.dispose()
        app.dispose()

        oauthCode.blankThenThrow("OAuth code")
    }

    private fun <T> List<T>.toPair(): Pair<T, T> = Pair(get(0), get(1))

    companion object {
        private const val serialVersionUID = 1L
    }
}

