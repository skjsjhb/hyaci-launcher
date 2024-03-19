package skjsjhb.mc.hyaci.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI

object Requests {
    /**
     * Retrieves arbitrary URL content as string.
     */
    fun getString(url: String): String = URI(url).toURL().openStream().use {
        InputStreamReader(it).readText()
    }

    /**
     * Retrieves arbitrary URL content and parses it as JSON document.
     */
    fun getJson(url: String): JsonElement = Json.parseToJsonElement(getString(url))

    /**
     * Retrieves content over HTTP as string, with HTTP headers.
     */
    fun getString(url: String, headers: Map<String, String>): String =
        openHttpConnection(url).run {
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            InputStreamReader(inputStream).use { it.readText() }
        }

    /**
     * Post the given content over HTTP, and gets the response body as string.
     *
     * Responses are returned as-is, regardless of the response code.
     */
    fun postString(url: String, headers: Map<String, String>, content: String): String =
        openHttpConnection(url).run {
            requestMethod = "POST"
            doOutput = true

            headers.forEach { (k, v) -> setRequestProperty(k, v) }

            OutputStreamWriter(outputStream).use { it.write(content) }
            InputStreamReader(inputStream).use { it.readText() }
        }

    /**
     * Post the content as JSON over HTTP, and gets the response body as JSON.
     */
    fun postJson(url: String, headers: Map<String, String>, content: JsonElement): JsonElement {
        val jsonHeaders = mutableMapOf<String, String>().apply {
            putAll(headers)
            put("Content-Type", "application/json")
            put("Accept", "application/json")
        }
        return Json.parseToJsonElement(postString(url, jsonHeaders, content.toString()))
    }

    /**
     * Post the content as JSON over HTTP, with default headers, and gets the response body as JSON.
     */
    fun postJson(url: String, content: JsonElement): JsonElement = postJson(url, emptyMap(), content)

    // Checks and opens an HTTP connection
    private fun openHttpConnection(url: String): HttpURLConnection =
        URI(url).toURL().openConnection().also {
            if (it !is HttpURLConnection) throw IllegalArgumentException("Not an HTTP URL: $url")
        } as HttpURLConnection
}