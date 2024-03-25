@file:JvmName("OAuthHelper")

package skjsjhb.mc.hyaci.auth

import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CountDownLatch
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    println("Hyaci OAuth Helper 1.0")

    val (cachePath, cefPath) = args

    println("Cache path: $cachePath")
    println("CEF path: $cefPath")

    println("Initializing browser.")
    val app = CefAppBuilder().apply {
        setInstallDir(File(cefPath))

        cefSettings.windowless_rendering_enabled = false
        cefSettings.cache_path = Path.of(cachePath).toAbsolutePath().normalize().toString()
        cefSettings.locale = Locale.getDefault().toLanguageTag()

        setAppHandler(object : MavenCefAppHandlerAdapter() {
            override fun stateHasChanged(state: CefApp.CefAppState?) {
                if (state == CefApp.CefAppState.TERMINATED) exitProcess(0)
            }
        })
    }.build()

    println("Creating client and browser instances.")
    val client = app.createClient()
    val router = CefMessageRouter.create()
    client.addMessageRouter(router)

    val url =
        "https://login.live.com/oauth20_authorize.srf?client_id=00000000402b5328&response_type=code&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"
    val browser = client.createBrowser(url, false, false)

    lateinit var frame: JFrame

    val latch = CountDownLatch(1)

    val loginCompletePrefix = "https://login.live.com/oauth20_desktop.srf"
    client.addLoadHandler(object : CefLoadHandlerAdapter() {
        override fun onLoadStart(
            browser: CefBrowser,
            frame0: CefFrame,
            transition0: CefRequest.TransitionType
        ) {
            if (browser.url.startsWith(loginCompletePrefix)) {
                println("Code retrieved, parsing.")
                val oauthCode = URI(browser.url).toURL().query
                    .split("&")
                    .associate {
                        it.split("=").let { (k, v) -> k to v }
                    }["code"] ?: ""
                println("==== DO NOT SHARE THE CODE BELOW ==== ")
                println("OAuth Code: $oauthCode") // Exchange with caller
                println("==== DO NOT SHARE THE CODE ABOVE ====")
                latch.countDown()
            }
        }
    })


    SwingUtilities.invokeAndWait {
        frame = JFrame("Login").apply {
            add(browser.uiComponent)
            pack()
            size = Toolkit.getDefaultToolkit().screenSize.let {
                Dimension((it.width * 0.6).toInt(), (it.height * 0.6).toInt())
            }
            setLocationRelativeTo(null)
            isVisible = true
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent?) {
                    latch.countDown()
                }
            })
        }
    }
    println("Initialized, waiting for user login.")
    latch.await()
    println("Code printed. Exiting.")
    SwingUtilities.invokeAndWait {
        frame.isVisible = false
        frame.dispose()
    }
    app.dispose()
}
