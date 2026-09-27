package com.nexus.messenger.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object Api {
    private val exec = Executors.newCachedThreadPool()

    private const val URL_DISPATCHER = "https://fifenya.github.io/nexus-redirect/url.txt"

    fun resolveServer(onDone: () -> Unit) {
        exec.execute {
            try {
                val conn = URL("$URL_DISPATCHER?nocache=${System.currentTimeMillis()}")
                    .openConnection() as HttpURLConnection
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
            }
            onDone()
        }
    }

    fun friendlyError(body: String): String {
        return when {
            body.contains("error code: 1033") ->
                "Туннель пересоздаётся (1033). Подождите ~1 минуту и повторите."
            body.contains("error code: 1027") ->
                "Слишком много запросов к туннелю (1027). Подождите пару минут."
            body.contains("error code: 10") ->
                "Cloudflare недоступен. Проверьте интернет."
            body.trim().startsWith("<") ->
                "Сервер вернул не JSON — туннель, скорее всего, мёртв."
            body.isEmpty() ->
                "Нет ответа от сервера."
            else -> body
        }
    }

    private fun request(
        method: String, path: String, body: JSONObject? = null,
        onResult: (Int, String) -> Unit
    ) {
        if (Store.testMode) {
            MockServer.handle(method, path, body, onResult)
            return
        }
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

    /** Multipart-загрузка файла (для мотов) */
    fun uploadFile(ctx: Context, uri: Uri, onResult: (Int, String) -> Unit) {
        if (Store.testMode) {
            val url = "mock://mote/${System.currentTimeMillis() % 100000}"
            onResult(201, JSONObject().apply {
                put("url", url)
                put("type", "image")
                put("size", 1024)
                put("mimeType", "image/jpeg")
            }.toString())
            return
        }
        exec.execute {
            try {
                val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    onResult(0, "Не удалось прочитать файл")
                    return@execute
                }
                val boundary = "----NexusBoundary" + System.currentTimeMillis()
                val conn = URL(Store.apiBase + "/uploads").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 20000
                conn.readTimeout = 30000
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                Store.token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
                val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"

                conn.outputStream.use { os ->
                    val head = ("--$boundary\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"mote.jpg\"\r\n" +
                        "Content-Type: $mime\r\n\r\n").toByteArray()
                    os.write(head)
                    os.write(bytes)
                    os.write("\r\n--$boundary--\r\n".toByteArray())
                    os.flush()
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.let {
                    BufferedReader(InputStreamReader(it)).use { r -> r.readText() }
                } ?: ""
                onResult(code, text)
            } catch (e: Exception) {
                onResult(0, e.message ?: "upload error")
            }
        }
    }

    fun parseArray(s: String): JSONArray = try { JSONArray(s) } catch (_: Exception) { JSONArray() }
    fun parseObj(s: String): JSONObject? = try { JSONObject(s) } catch (_: Exception) { null }
}