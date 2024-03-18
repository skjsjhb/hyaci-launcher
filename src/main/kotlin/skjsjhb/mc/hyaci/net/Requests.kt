package skjsjhb.mc.hyaci.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI

object Requests {
    /**
     * Retrieves content as string.
     *
     * This method is not an overload of [getString] with headers.
     * The latter one only resolves HTTP requests.
     */
    fun getString(url: String): String = URI(url).toURL().openStream().use {
        InputStreamReader(it).readText()
    }

    /**
     * Retrieves content as string, with HTTP headers.
     */
    fun getString(url: String, headers: Map<String, String>): String {
        val connection = URI(url).toURL().openConnection().also {
            if (it !is HttpURLConnection) throw IOException("Cannot GET from $url")
        } as HttpURLConnection
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        return InputStreamReader(connection.inputStream).use { it.readText() }
    }

    /**
     * Post the given content, and gets the response body as string.
     */
    fun postString(url: String, headers: Map<String, String>, content: String): String {
        val connection = URI(url).toURL().openConnection().also {
            if (it !is HttpURLConnection) throw IOException("Cannot POST to $url")
        } as HttpURLConnection
        connection.requestMethod = "POST"
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        connection.doOutput = true
        OutputStreamWriter(connection.outputStream).use { it.write(content) }
        return InputStreamReader(connection.inputStream).use { it.readText() }
    }

    /**
     * Post the content as JSON, and gets the response body as JSON.
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
     * Post the content as JSON with default headers, and gets the response body as JSON.
     */
    fun postJson(url: String, content: JsonElement): JsonElement = postJson(url, emptyMap(), content)
}