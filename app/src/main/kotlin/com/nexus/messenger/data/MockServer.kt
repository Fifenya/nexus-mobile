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
        for (url in m.attachments) {
            val attObj = JSONObject()
            attObj.put("type", "image")
            attObj.put("url", url)
            attArray.put(attObj)
        }
        val viewsArray = JSONArray()
        for (uid in m.views) {
            val userObj = JSONObject()
            userObj.put("id", uid)
            userObj.put("username", BOT_NAME)
            val viewObj = JSONObject()
            viewObj.put("user", userObj)
            viewsArray.put(viewObj)
        }
        val authorObj = JSONObject()
        authorObj.put("id", m.authorId)
        authorObj.put("username", m.authorName)
        val out = JSONObject()
        out.put("id", m.id)
        out.put("text", m.text)
        out.put("createdAt", iso(m.ts))
        out.put("author", authorObj)
        out.put("views", viewsArray)
        out.put("attachments", attArray)
        return out
    }

    private fun moteJson(m: MoteM): JSONObject {
        val out = JSONObject()
        out.put("id", m.id)
        out.put("name", m.name)
        out.put("url", m.url)
        out.put("tags", "")
        out.put("ownerId", m.ownerId)
        return out
    }

    private fun chatJson(id: String, title: String, type: String): JSONObject {
        val list = chats[id] ?: mutableListOf()
        val last = list.lastOrNull()
        val out = JSONObject()
        out.put("id", id)
        out.put("title", title)
        out.put("type", type)
        out.put("pinned", false)
        out.put("unreadCount", 0)
        if (last != null) {
            val lm = JSONObject()
            lm.put("text", last.text.ifEmpty { "📎 Мот" })
            lm.put("createdAt", iso(last.ts))
            out.put("lastMessage", lm)
        } else {
            out.put("lastMessage", JSONObject.NULL)
        }
        return out
    }

    fun handle(method: String, path: String, body: JSONObject?, onResult: (Int, String) -> Unit) {
        exec.schedule({
            val result = route(method, path, body)
            onResult(result.first, result.second)
        }, 80, TimeUnit.MILLISECONDS)
    }

    private fun route(method: String, path: String, body: JSONObject?): Pair<Int, String> {
        return when {
            method == "GET" && path == "/chats" -> {
                val arr = JSONArray()
                arr.put(chatJson("mock-echo", "Эхо-чат", "PRIVATE"))
                arr.put(chatJson("mock-team", "Команда Nexus", "GROUP"))
                arr.put(chatJson("mock-news", "Nexus News", "GROUP"))
                Pair(200, arr.toString())
            }

            method == "GET" && path.startsWith("/chats/") && path.endsWith("/messages") -> {
                val id = path.removePrefix("/chats/").removeSuffix("/messages")
                val arr = JSONArray()
                val list = chats[id] ?: mutableListOf()
                for (m in list) arr.put(mJson(m))
                Pair(200, arr.toString())
            }

            method == "POST" && path.startsWith("/chats/") && path.endsWith("/messages") -> {
                val id = path.removePrefix("/chats/").removeSuffix("/messages")
                val text = body?.optString("text") ?: ""
                val atts = mutableListOf<String>()
                val attArr = body?.optJSONArray("attachments")
                if (attArr != null) {
                    for (i in 0 until attArr.length()) {
                        val u = attArr.optJSONObject(i)?.optString("url")
                        if (u != null && u.isNotEmpty()) atts.add(u)
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
                Pair(201, mJson(mine).toString())
            }

            method == "POST" && path == "/view" ->
                Pair(200, "{\"ok\":true}")

            method == "PATCH" && path.startsWith("/messages/") -> {
                val id = path.removePrefix("/messages/")
                var edited: M? = null
                for (list in chats.values) {
                    for (m in list) {
                        if (m.id == id) {
                            m.text = body?.optString("text") ?: m.text
                            edited = m
                            break
                        }
                    }
                    if (edited != null) break
                }
                if (edited != null) Pair(200, mJson(edited).toString())
                else Pair(404, "{\"statusCode\":404,\"message\":\"Mock: message not found\"}")
            }

            method == "DELETE" && path.startsWith("/messages/") -> {
                val id = path.removePrefix("/messages/")
                for (list in chats.values) {
                    list.removeAll { it.id == id }
                }
                Pair(200, "{\"ok\":true}")
            }

            method == "POST" && path.matches(Regex("/messages/[^/]+/reactions")) ->
                Pair(200, "{\"ok\":true}")

            method == "GET" && path.startsWith("/motes/gallery") -> {
                val q = path.substringAfter("search=", "").substringBefore("&").lowercase()
                val filtered = if (q.isEmpty()) motes else motes.filter { it.name.lowercase().contains(q) }
                val arr = JSONArray()
                for (mote in filtered) arr.put(moteJson(mote))
                Pair(200, arr.toString())
            }

            method == "POST" && path == "/motes/gallery" -> {
                val name = body?.optString("name") ?: "Мот"
                val url = body?.optString("url") ?: "mock://mote/$counter"
                val m = MoteM("mote-${++counter}", name, url, MY_ID)
                motes.add(0, m)
                Pair(201, moteJson(m).toString())
            }

            method == "DELETE" && path.startsWith("/motes/gallery/") -> {
                val id = path.removePrefix("/motes/gallery/")
                motes.removeAll { it.id == id }
                Pair(200, "{\"ok\":true}")
            }

            method == "GET" && path.matches(Regex("/chats/[^/]+/members")) -> {
                val chatId = path.removePrefix("/chats/").removeSuffix("/members")
                val arr = JSONArray()
                val memberList = members[chatId] ?: mutableListOf()
                for (m in memberList) {
                    val uid: String = m["id"] ?: ""
                    val uname: String = m["username"] ?: ""
                    val urole: String = m["role"] ?: ""
                    val userObj = JSONObject()
                    userObj.put("id", uid)
                    userObj.put("username", uname)
                    val rowObj = JSONObject()
                    rowObj.put("id", "m$uid")
                    rowObj.put("role", urole)
                    rowObj.put("user", userObj)
                    arr.put(rowObj)
                }
                Pair(200, arr.toString())
            }

            method == "POST" && path.matches(Regex("/chats/[^/]+/members")) -> {
                val chatId = path.removePrefix("/chats/").removeSuffix("/members")
                val ids = body?.optJSONArray("userIds") ?: JSONArray()
                val list = members.getOrPut(chatId) { mutableListOf() }
                for (i in 0 until ids.length()) {
                    val uid = ids.optString(i)
                    if (uid.isNotEmpty()) {
                        val exists = list.any { it["id"] == uid }
                        if (!exists) {
                            list.add(mapOf("id" to uid, "username" to uid, "role" to "MEMBER"))
                        }
                    }
                }
                Pair(200, "{\"added\":${ids.length()}}")
            }

            method == "DELETE" && path.matches(Regex("/chats/[^/]+/members/[^/]+")) -> {
                val parts = path.removePrefix("/chats/").split("/members/")
                val chatId = parts[0]
                val userId = parts[1]
                val list = members[chatId]
                if (list != null) {
                    list.removeAll { it["id"] == userId }
                }
                Pair(200, "{\"ok\":true}")
            }

            method == "POST" && path.matches(Regex("/chats/[^/]+/leave")) -> {
                val chatId = path.removePrefix("/chats/").removeSuffix("/leave")
                val list = members[chatId]
                if (list != null) {
                    list.removeAll { it["id"] == MY_ID }
                }
                Pair(200, "{\"ok\":true}")
            }

            method == "GET" && path.matches(Regex("/chats/[^/]+/settings")) -> {
                val chatId = path.removePrefix("/chats/").removeSuffix("/settings")
                val cur = chatSettings[chatId] ?: run {
                    val perms = JSONObject()
                    perms.put("sendMessages", "all")
                    perms.put("inviteUsers", "all")
                    perms.put("pinMessages", "admin")
                    val obj = JSONObject()
                    obj.put("reactions", "all")
                    obj.put("slowMode", 0)
                    obj.put("permissions", perms)
                    obj
                }
                Pair(200, cur.toString())
            }

            method == "PATCH" && path.matches(Regex("/chats/[^/]+/settings")) -> {
                val chatId = path.removePrefix("/chats/").removeSuffix("/settings")
                val cur = chatSettings.getOrPut(chatId) { JSONObject() }
                val keys = body?.keys()
                if (keys != null) {
                    while (keys.hasNext()) {
                        val k = keys.next()
                        cur.put(k, body.opt(k))
                    }
                }
                Pair(200, cur.toString())
            }

            method == "PATCH" && path.matches(Regex("/chats/[^/]+$")) -> {
                val out = JSONObject()
                out.put("id", path.removePrefix("/chats/"))
                out.put("title", body?.optString("title") ?: "")
                Pair(200, out.toString())
            }

            method == "GET" && path.matches(Regex("/chats/[^/]+/invites")) -> {
                val chatId = path.removePrefix("/chats/").removeSuffix("/invites")
                val arr = JSONArray()
                for (inv in invites) {
                    if (inv.chatId == chatId) {
                        val creator = JSONObject()
                        creator.put("username", MY_NAME)
                        val rowObj = JSONObject()
                        rowObj.put("id", inv.id)
                        rowObj.put("code", inv.code)
                        rowObj.put("uses", inv.uses)
                        rowObj.put("creator", creator)
                        arr.put(rowObj)
                    }
                }
                Pair(200, arr.toString())
            }

            method == "POST" && path.matches(Regex("/chats/[^/]+/invites")) -> {
                val chatId = path.removePrefix("/chats/").removeSuffix("/invites")
                val inv = Invite(nextInviteId(), "mock-${(1000..9999).random()}", chatId)
                invites.add(inv)
                val out = JSONObject()
                out.put("id", inv.id)
                out.put("code", inv.code)
                out.put("uses", 0)
                Pair(200, out.toString())
            }

            method == "DELETE" && path.matches(Regex("/chats/[^/]+/invites/[^/]+")) -> {
                val parts = path.removePrefix("/chats/").split("/invites/")
                val invId = parts[1]
                invites.removeAll { it.id == invId }
                Pair(200, "{\"ok\":true}")
            }

            method == "POST" && path.matches(Regex("/chats/invites/[^/]+/join")) ->
                Pair(200, "{\"ok\":true}")

            method == "POST" && path == "/auth/forgot" -> {
                val out = JSONObject()
                out.put("ok", true)
                out.put("message", "Тест-режим: код 123456 «отправлен» в Моты")
                Pair(200, out.toString())
            }

            method == "POST" && path == "/auth/reset" -> {
                val code = body?.optString("code") ?: ""
                if (code == "123456") Pair(200, "{\"ok\":true,\"message\":\"Пароль изменён (тест)\"}")
                else Pair(400, "{\"statusCode\":400,\"message\":\"Неверный код\"}")
            }

            method == "GET" && path == "/users/search" -> {
                val arr = JSONArray()
                val c = JSONObject()
                c.put("id", "mock-carol")
                c.put("username", "Carol")
                c.put("displayName", "Carol")
                arr.put(c)
                val d = JSONObject()
                d.put("id", "mock-dave")
                d.put("username", "Dave")
                d.put("displayName", "Dave")
                arr.put(d)
                Pair(200, arr.toString())
            }

            method == "GET" && path == "/users/me" ->
                Pair(200, userJson(MY_ID, MY_NAME, "online"))

            method == "PATCH" && path == "/users/me" ->
                Pair(200, userJson(MY_ID, body?.optString("displayName") ?: MY_NAME, "online"))

            method == "GET" && path.startsWith("/users/") ->
                Pair(200, userJson(BOT_ID, BOT_NAME, "online"))

            else ->
                Pair(404, "{\"statusCode\":404,\"message\":\"Mock: no route for $method $path\"}")
        }
    }

    private fun userJson(id: String, name: String, status: String): String {
        val out = JSONObject()
        out.put("id", id)
        out.put("username", name)
        out.put("displayName", name)
        out.put("onlineStatus", status)
        out.put("online", status == "online")
        out.put("bio", "Локальный пользователь тест-режима")
        return out.toString()
    }
}