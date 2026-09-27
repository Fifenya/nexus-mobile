package com.nexus.messenger.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object MockServer {
    private const val MY_ID = "test-user"
    private const val MY_NAME = "testdevapp"
    private const val BOT_ID = "nexus-bot"
    private const val BOT_NAME = "Nexus Bot"
    private const val ALICE_ID = "mock-alice"
    private const val ALICE_NAME = "Alice"
    private const val BOB_ID = "mock-bob"
    private const val BOB_NAME = "Bob"

    private class M(
        val id: String,
        var text: String,
        val authorId: String,
        val authorName: String,
        val ts: Long,
        val views: MutableList<String> = mutableListOf(),
        val attachments: MutableList<String> = mutableListOf()
    )

    private class MoteM(
        val id: String,
        var name: String,
        val url: String,
        val ownerId: String
    )

    private class Invite(val id: String, val code: String, val chatId: String, var uses: Int = 0)

    private val chats = mutableMapOf<String, MutableList<M>>()
    private val members = mutableMapOf<String, MutableList<Map<String, String>>>()
    private val chatSettings = mutableMapOf<String, JSONObject>()
    private val invites = mutableListOf<Invite>()
    private val motes = mutableListOf<MoteM>()
    private val exec = Executors.newSingleThreadScheduledExecutor()
    private var counter = 0

    init {
        val now = System.currentTimeMillis()
        chats["mock-echo"] = mutableListOf(
            M(nextId(), "Добро пожаловать в тест-режим Nexus! Это эхо-чат: отправь сообщение — я верну ответ.", BOT_ID, BOT_NAME, now - 3600_000)
        )
        members["mock-echo"] = mutableListOf(
            mapOf("id" to MY_ID, "username" to MY_NAME, "role" to "MEMBER"),
            mapOf("id" to BOT_ID, "username" to BOT_NAME, "role" to "OWNER")
        )
        chats["mock-team"] = mutableListOf(
            M(nextId(), "Всем привет! Это локальный тестовый чат команды.", ALICE_ID, ALICE_NAME, now - 7200_000),
            M(nextId(), "Держите мот дня ⭐", BOB_ID, BOB_NAME, now - 5400_000, attachments = mutableListOf("mock://mote/7")),
            M(nextId(), "Тест-режим работает полностью офлайн.", BOT_ID, BOT_NAME, now - 1800_000)
        )
        members["mock-team"] = mutableListOf(
            mapOf("id" to MY_ID, "username" to MY_NAME, "role" to "OWNER"),
            mapOf("id" to ALICE_ID, "username" to ALICE_NAME, "role" to "ADMIN"),
            mapOf("id" to BOB_ID, "username" to BOB_NAME, "role" to "MEMBER"),
            mapOf("id" to BOT_ID, "username" to BOT_NAME, "role" to "MEMBER")
        )
        chats["mock-news"] = mutableListOf(
            M(nextId(), "Nexus Mobile v1.0: тёмно-красная тема, кастомные диалоги, тест-режим.", BOT_ID, BOT_NAME, now - 86400_000)
        )
        members["mock-news"] = mutableListOf(
            mapOf("id" to MY_ID, "username" to MY_NAME, "role" to "OWNER"),
            mapOf("id" to BOT_ID, "username" to BOT_NAME, "role" to "MEMBER")
        )

        motes.add(MoteM("mote-1", "Красная звезда", "mock://mote/1", BOT_ID))
        motes.add(MoteM("mote-2", "Фиолетовый вихрь", "mock://mote/2", BOT_ID))
        motes.add(MoteM("mote-3", "Бирюзовый кот", "mock://mote/3", ALICE_ID))
        motes.add(MoteM("mote-4", "Огненный мот", "mock://mote/4", MY_ID))
        motes.add(MoteM("mote-5", "Зелёный лист", "mock://mote/5", BOB_ID))
        motes.add(MoteM("mote-6", "Розовый пончик", "mock://mote/6", BOT_ID))
    }

    private fun nextId(): String = "mock-m${++counter}"
    private fun nextInviteId(): String = "mock-i${++counter}"
    private fun iso(ts: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(ts))

    private fun mJson(m: M): JSONObject {
        val attArray = JSONArray()
        m.attachments.forEach { url ->
            attArray.put(JSONObject().put("type", "image").put("url", url))
        }
        val viewsArray = JSONArray()
        m.views.forEach { uid ->
            viewsArray.put(JSONObject().put("user", JSONObject().put("id", uid).put("username", BOT_NAME)))
        }
        return JSONObject().apply {
            put("id", m.id)
            put("text", m.text)
            put("createdAt", iso(m.ts))
            put("author", JSONObject().put("id", m.authorId).put("username", m.authorName))
            put("views", viewsArray)
            put("attachments", attArray)
        }
    }

    private fun moteJson(m: MoteM): JSONObject = JSONObject().apply {
        put("id", m.id)
        put("name", m.name)
        put("url", m.url)
        put("tags", "")
        put("ownerId", m.ownerId)
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
                    put("text", last.text.ifEmpty { "📎 Мот" })
                    put("createdAt", iso(last.ts))
                })
            } else {
                put("lastMessage", JSONObject.NULL)
            }
        }
    }

    fun handle(method: String, path: String, body: JSONObject?, onResult: (Int, String) -> Unit) {
        exec.schedule({
            val (code, text) = route(method, path, body)
            onResult(code, text)
        }, 80, TimeUnit.MILLISECONDS)
    }

    private fun route(method: String, path: String, body: JSONObject?): Pair<Int, String> {
        return when {
            method == "GET" && path == "/chats" -> {
                val arr = JSONArray()
                arr.put(chatJson("mock-echo", "Эхо-чат", "PRIVATE"))
                arr.put(chatJson("mock-team", "Команда Nexus", "GROUP"))
                arr.put(chatJson("mock-news", "Nexus News", "GROUP"))
                200 to arr.toString()
            }

            method == "GET" && path.startsWith("/chats/") && path.endsWith("/messages") -> {
                val id = path.removePrefix("/chats/").removeSuffix("/messages")
                val arr = JSONArray()
                (chats[id] ?: mutableListOf()).forEach { arr.put(mJson(it)) }
                200 to arr.toString()
            }

            method == "POST" && path.startsWith("/chats/") && path.endsWith("/messages") -> {
                val id = path.removePrefix("/chats/").removeSuffix("/messages")
                val text = body?.optString("text") ?: ""
                val atts = mutableListOf<String>()
                val attArr = body?.optJSONArray("attachments")
                if (attArr != null) {
                    for (i in 0 until attArr.length()) {
                        val u = attArr.optJSONObject(i)?.optString("url")
                        if (!u.isNullOrEmpty()) atts.add(u)
                    }
                }
                val list = chats.getOrPut(id) { mutableListOf() }
                val mine = M(nextId(), text, MY_ID, MY_NAME, System.currentTimeMillis(), attachments = atts)
                list.add(mine)
                if (id == "mock-echo") {
                    mine.views.add(BOT_ID)
                    val latency = (18..90).random()
                    val echoText = if (atts.isNotEmpty())
                        "📡 echo-server: принят мот · status=200 · latency=${latency}ms"
                    else
                        "📡 echo-server: принято «${text}» · status=200 · latency=${latency}ms · msgId=${mine.id}"
                    list.add(M(nextId(), echoText, BOT_ID, BOT_NAME, System.currentTimeMillis() + 1))
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

            method == "POST" && path.matches(Regex("/messages/[^/]+/reactions")) ->
                200 to "{\"ok\":true}"

            method == "GET" && path.startsWith("/motes/gallery") -> {
                val q = path.substringAfter("search=", "").substringBefore("&").lowercase()
                val filtered = if (q.isEmpty()) motes else motes.filter { it.name.lowercase().contains(q) }
                val arr = JSONArray()
                filtered.forEach { arr.put(moteJson(it)) }
                200 to arr.toString()
            }

            method == "POST" && path == "/motes/gallery" -> {
                val m = MoteM(
                    "mote-${++counter}",
                    body?.optString("name") ?: "Мот",
                    body?.optString("url") ?: "mock://mote/$counter",
                    MY_ID
                )
                motes.add(0, m)
                201 to moteJson(m).toString()
            }

            method == "DELETE" && path.startsWith("/motes/gallery/") -> {
                val id = path.removePrefix("/motes/gallery/")
                motes.removeAll { it.id == id }
                200 to "{\"ok\":true}"
            }

            method == "GET" && path.matches(Regex("/chats/[^/]+/members")) -> {
                val id = path.removePrefix("/chats/").removeSuffix("/members")
                val arr = JSONArray()
                (members[id] ?: mutableListOf()).forEach { m ->
                    arr.put(JSONObject().apply {
                        put("id", "m${m["id"]}")
                        put("role", m["role"])
                        put("user", JSONObject().apply {
                            put("id", m["id"])
                            put("username", m["username"])
                        })
                    })
                }
                200 to arr.toString()
            }

            method == "POST" && path.matches(Regex("/chats/[^/]+/members")) -> {
                val id = path.removePrefix("/chats/").removeSuffix("/members")
                val ids = body?.optJSONArray("userIds") ?: JSONArray()
                val list = members.getOrPut(id) { mutableListOf() }
                for (i in 0 until ids.length()) {
                    val uid = ids.optString(i)
                    if (uid.isNotEmpty() && list.none { it["id"] == uid }) {
                        list.add(mapOf("id" to uid, "username" to uid, "role" to "MEMBER"))
                    }
                }
                200 to "{\"added\":${ids.length()}}"
            }

            method == "DELETE" && path.matches(Regex("/chats/[^/]+/members/[^/]+")) -> {
                val parts = path.removePrefix("/chats/").split("/members/")
                members[parts[0]]?.removeAll { it["id"] == parts[1] }
                200 to "{\"ok\":true}"
            }

            method == "POST" && path.matches(Regex("/chats/[^/]+/leave")) -> {
                val id = path.removePrefix("/chats/").removeSuffix("/leave")
                members[id]?.removeAll { it["id"] == MY_ID }
                200 to "{\"ok\":true}"
            }

            method == "GET" && path.matches(Regex("/chats/[^/]+/settings")) -> {
                val id = path.removePrefix("/chats/").removeSuffix("/settings")
                val cur = chatSettings[id] ?: JSONObject().apply {
                    put("reactions", "all")
                    put("slowMode", 0)
                    put("permissions", JSONObject().apply {
                        put("sendMessages", "all")
                        put("inviteUsers", "all")
                        put("pinMessages", "admin")
                    })
                }
                200 to cur.toString()
            }

            method == "PATCH" && path.matches(Regex("/chats/[^/]+/settings")) -> {
                val id = path.removePrefix("/chats/").removeSuffix("/settings")
                val cur = chatSettings.getOrPut(id) { JSONObject() }
                body?.keys()?.forEach { k -> cur.put(k, body.opt(k)) }
                200 to cur.toString()
            }

            method == "PATCH" && path.matches(Regex("/chats/[^/]+$")) ->
                200 to "{\"id\":\"${path.removePrefix("/chats/")}\",\"title\":\"${body?.optString("title") ?: ""}\"}"

            method == "GET" && path.matches(Regex("/chats/[^/]+/invites")) -> {
                val id = path.removePrefix("/chats/").removeSuffix("/invites")
                val arr = JSONArray()
                invites.filter { it.chatId == id }.forEach { inv ->
                    arr.put(JSONObject().apply {
                        put("id", inv.id)
                        put("code", inv.code)
                        put("uses", inv.uses)
                        put("creator", JSONObject().put("username", MY_NAME))
                    })
                }
                200 to arr.toString()
            }

            method == "POST" && path.matches(Regex("/chats/[^/]+/invites")) -> {
                val id = path.removePrefix("/chats/").removeSuffix("/invites")
                val inv = Invite(nextInviteId(), "mock-${(1000..9999).random()}", id)
                invites.add(inv)
                200 to JSONObject().apply {
                    put("id", inv.id)
                    put("code", inv.code)
                    put("uses", 0)
                }.toString()
            }

            method == "DELETE" && path.matches(Regex("/chats/[^/]+/invites/[^/]+")) -> {
                val parts = path.removePrefix("/chats/").split("/invites/")
                invites.removeAll { it.id == parts[1] }
                200 to "{\"ok\":true}"
            }

            method == "POST" && path.matches(Regex("/chats/invites/[^/]+/join")) ->
                200 to "{\"ok\":true}"

            method == "POST" && path == "/auth/forgot" ->
                200 to JSONObject().apply {
                    put("ok", true)
                    put("message", "Тест-режим: код 123456 «отправлен» в Моты")
                }.toString()

            method == "POST" && path == "/auth/reset" -> {
                val code = body?.optString("code") ?: ""
                if (code == "123456") 200 to "{\"ok\":true,\"message\":\"Пароль изменён (тест)\"}"
                else 400 to "{\"statusCode\":400,\"message\":\"Неверный код\"}"
            }

            method == "GET" && path == "/users/search" -> {
                val arr = JSONArray()
                arr.put(JSONObject().apply { put("id", "mock-carol"); put("username", "Carol"); put("displayName", "Carol") })
                arr.put(JSONObject().apply { put("id", "mock-dave"); put("username", "Dave"); put("displayName", "Dave") })
                200 to arr.toString()
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