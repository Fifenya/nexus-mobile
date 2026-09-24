package com.nexus.messenger.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object Api {
    private val exec = Executors.newCachedThreadPool()

    // Стабильная ссылка-диспетчер (GitHub Pages)
    private const val URL_DISPATCHER = "https://fifenya.github.io/nexus-redirect/url.txt"

    /**
     * При старте получаем актуальный URL туннеля с GitHub Pages.
     * Если не получилось — оставляем то, что сохранено в Store.
     */
    fun resolveServer(onDone: () -> Unit) {
        exec.execute {
            try {
                val conn = URL(URL_DISPATCHER).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                if (conn.responseCode == 200) {
                    val text = BufferedReader(InputStreamReader(conn.inputStream))
                        .use { it.readText() }.trim()
                    if (text.startsWith("https://") || text.startsWith("http://")) {
                        Store.apiBase = text
                    }
                }
            } catch (_: Exception) {
                // fallback — используем сохранённый адрес
            }
            onDone()
        }
    }

    private fun request(
        method: String, path: String, body: JSONObject? = null,
        onResult: (Int, String) -> Unit
    ) {
        exec.execute {
            try {
                val url = URL(Store.apiBase + path)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = method
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/json")
                Store.token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }

                if (body != null) {
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toString().toByteArray()) }
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.let {
                    BufferedReader(InputStreamReader(it)).use { r -> r.readText() }
                } ?: ""
                onResult(code, text)
            } catch (e: Exception) {
                onResult(0, e.message ?: "network error")
            }
        }
    }

    fun get(path: String, cb: (Int, String) -> Unit) = request("GET", path, null, cb)
    fun post(path: String, body: JSONObject, cb: (Int, String) -> Unit) = request("POST", path, body, cb)
    fun patch(path: String, body: JSONObject, cb: (Int, String) -> Unit) = request("PATCH", path, body, cb)
    fun delete(path: String, cb: (Int, String) -> Unit) = request("DELETE", path, null, cb)

    fun parseArray(s: String): JSONArray = try { JSONArray(s) } catch (_: Exception) { JSONArray() }
    fun parseObj(s: String): JSONObject? = try { JSONObject(s) } catch (_: Exception) { null }
}
