@file:JvmName("VanillaAccountHelper")

package skjsjhb.mc.hyaci.auth

import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import skjsjhb.mc.hyaci.sys.dataPathOf
import skjsjhb.mc.hyaci.util.Sources
import skjsjhb.mc.hyaci.util.debug
import skjsjhb.mc.hyaci.util.info
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.net.URI
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame
import javax.swing.WindowConstants

fun main(args: Array<String>) {
    val internalId = args[0]
    lateinit var oauthCode: String

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

    System.err.println("OAuth Code: $oauthCode")
}

private fun <T> List<T>.toPair(): Pair<T, T> = Pair(get(0), get(1))