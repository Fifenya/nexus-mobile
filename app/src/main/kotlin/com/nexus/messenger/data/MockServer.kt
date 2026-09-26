package com.nexus.messenger.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Локальный эхо-бэкенд для тест-режима (testdevapp / testdevapp) */
object MockServer {
    private const val MY_ID = "test-user"
    private const val MY_NAME = "testdevapp"
    private const val BOT_ID = "nexus-bot"
    private const val BOT_NAME = "Nexus Bot"

    private class M(
        val id: String,
        var text: String,
        val authorId: String,
        val authorName: String,
        val ts: Long,
        val views: MutableList<String> = mutableListOf()
    )

    private val chats = mutableMapOf<String, MutableList<M>>()
    private val exec = Executors.newSingleThreadScheduledExecutor()
    private var counter = 0

    init {
        val now = System.currentTimeMillis()
        chats["mock-echo"] = mutableListOf(
            M(nextId(), "Добро пожаловать в тест-режим Nexus! Это эхо-чат: отправь сообщение — я верну ответ, как будто его прислал сервер.", BOT_ID, BOT_NAME, now - 3600_000)
        )
        chats["mock-team"] = mutableListOf(
            M(nextId(), "Всем привет! Это локальный тестовый чат команды.", "alice", "Alice", now - 7200_000),
            M(nextId(), "Дизайн чатов уже похож на Telegram, осталось довести мелочи.", "bob", "Bob", now - 5400_000),
            M(nextId(), "Тест-режим работает полностью офлайн.", BOT_ID, BOT_NAME, now - 1800_000)
        )
        chats["mock-news"] = mutableListOf(
            M(nextId(), "Nexus Mobile v1.0: тёмно-красная тема, кастомные диалоги, тест-режим.", BOT_ID, BOT_NAME, now - 86400_000)
        )
    }

    private fun nextId(): String = "mock-m${++counter}"

    private fun iso(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(ts))

    private fun mJson(m: M): JSONObject = JSONObject().apply {
        put("id", m.id)
        put("text", m.text)
        put("createdAt", iso(m.ts))
        put("author", JSONObject().put("id", m.authorId).put("username", m.authorName))
        put("views", JSONArray(m.views.map {
            JSONObject().put("user", JSONObject().put("id", it).put("username", BOT_NAME))
        }))
    }

    private fun chatJson(id: String, title: String, type: String): JSONObject {
        val list = chats[id] ?: mutableListOf()
        val last = list.lastOrNull()
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("type", type)
            put("pinned", false)
            put("unreadCount", 0)
            if (last != null) {
                put("lastMessage", JSONObject().apply {
                    put("text", last.text)
                    put("createdAt", iso(last.ts))
                })
            } else {
                put("lastMessage", JSONObject.NULL)
            }
        }
    }

    fun handle(method: String, path: String, body: JSONObject?, onResult: (Int, String) -> Unit) {
        // небольшая задержка для реалистичности
        exec.schedule({
            val (code, text) = route(method, path, body)
            onResult(code, text)
        }, 120, TimeUnit.MILLISECONDS)
    }

    private fun route(method: String, path: String, body: JSONObject?): Pair<Int, String> {
        return when {
            method == "GET" && path == "/chats" ->
                200 to JSONArray().apply {
                    put(chatJson("mock-echo", "Эхо-чат", "PRIVATE"))
                    put(chatJson("mock-team", "Команда Nexus", "GROUP"))
                    put(chatJson("mock-news", "Nexus News", "GROUP"))
                }.toString()

            method == "GET" && path.startsWith("/chats/") && path.endsWith("/messages") -> {
                val id = path.removePrefix("/chats/").removeSuffix("/messages")
                200 to JSONArray((chats[id] ?: mutableListOf()).map { mJson(it) }).toString()
            }

            method == "POST" && path.startsWith("/chats/") && path.endsWith("/messages") -> {
                val id = path.removePrefix("/chats/").removeSuffix("/messages")
                val text = body?.optString("text") ?: ""
                val list = chats.getOrPut(id) { mutableListOf() }
                val mine = M(nextId(), text, MY_ID, MY_NAME, System.currentTimeMillis())
                list.add(mine)
                if (id == "mock-echo") {
                    // бот «прочитал» наше сообщение сразу → двойная галочка
                    mine.views.add(BOT_ID)
                    val latency = (18..90).random()
                    val reply = M(
                        nextId(),
                        "📡 echo-server: принято «${text}» · status=200 · latency=${latency}ms · msgId=${mine.id}",
                        BOT_ID, BOT_NAME, System.currentTimeMillis() + 1
                    )
                    list.add(reply)
                }
                201 to mJson(mine).toString()
            }

            method == "POST" && path == "/view" ->
                200 to "{\"ok\":true}"

            method == "PATCH" && path.startsWith("/messages/") -> {
                val id = path.removePrefix("/messages/")
                var edited: M? = null
                chats.values.forEach { list ->
                    list.firstOrNull { it.id == id }?.let {
                        it.text = body?.optString("text") ?: it.text
                        edited = it
                    }
                }
                edited?.let { 200 to mJson(it).toString() }
                    ?: 404 to "{\"statusCode\":404,\"message\":\"Mock: message not found\"}"
            }

            method == "DELETE" && path.startsWith("/messages/") -> {
                val id = path.removePrefix("/messages/")
                chats.values.forEach { list -> list.removeAll { it.id == id } }
                200 to "{\"ok\":true}"
            }

            method == "GET" && path == "/users/me" ->
                200 to userJson(MY_ID, MY_NAME, "online")

            method == "PATCH" && path == "/users/me" ->
                200 to userJson(MY_ID, body?.optString("displayName") ?: MY_NAME, "online")

            method == "GET" && path.startsWith("/users/") ->
                200 to userJson(BOT_ID, BOT_NAME, "online")

            else ->
                404 to "{\"statusCode\":404,\"message\":\"Mock: no route for $method $path\"}"
        }
    }

    private fun userJson(id: String, name: String, status: String): String =
        JSONObject().apply {
            put("id", id)
            put("username", name)
            put("displayName", name)
            put("onlineStatus", status)
            put("online", status == "online")
            put("bio", "Локальный пользователь тест-режима")
        }.toString()
}