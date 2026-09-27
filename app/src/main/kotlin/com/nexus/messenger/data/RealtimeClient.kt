package com.nexus.messenger.data

import android.content.Context
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.net.URI

// ─── DTO-события гейтвея ───

data class RtMessage(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val chatTitle: String?,
    val replyToId: String?,
    val attachments: List<String>
)

data class RtPresence(
    val userId: String,
    val status: String,
    val lastSeenAt: String?,
    val hidden: Boolean
)

data class RtTyping(
    val chatId: String,
    val userId: String,
    val isTyping: Boolean
)

data class RtChatUpdate(
    val id: String,
    val title: String?,
    val lastMessageText: String?,
    val lastMessageAt: String?
)

/**
 * Realtime-клиент под chat.gateway.ts:
 *   авторизация: handshake.auth.token
 *   комнаты: user:{userId}, chat:{chatId} (обе — на сервере при connect)
 *   входящие события: message:new / updated / deleted / reaction,
 *                     typing:update, presence:update, chat:updated
 *   исходящие: message:send / edit / delete / react,
 *              typing:start / stop, chat:join
 */
object RealtimeClient {
    const val STATE_CONNECTING = "connecting"
    const val STATE_ONLINE = "online"
    const val STATE_OFFLINE = "offline"

    private var socket: Socket? = null
    private var refs = 0

    @Volatile
    var state: String = STATE_OFFLINE
        private set

    private val messageNewL = mutableListOf<(RtMessage) -> Unit>()
    private val chatUpdateL = mutableListOf<(RtChatUpdate) -> Unit>()
    private val typingL = mutableListOf<(RtTyping) -> Unit>()
    private val presenceL = mutableListOf<(RtPresence) -> Unit>()

    fun addMessageListener(l: (RtMessage) -> Unit) { messageNewL.add(l) }
    fun removeMessageListener(l: (RtMessage) -> Unit) { messageNewL.remove(l) }
    fun addChatUpdateListener(l: (RtChatUpdate) -> Unit) { chatUpdateL.add(l) }
    fun removeChatUpdateListener(l: (RtChatUpdate) -> Unit) { chatUpdateL.remove(l) }
    fun addTypingListener(l: (RtTyping) -> Unit) { typingL.add(l) }
    fun removeTypingListener(l: (RtTyping) -> Unit) { typingL.remove(l) }
    fun addPresenceListener(l: (RtPresence) -> Unit) { presenceL.add(l) }
    fun removePresenceListener(l: (RtPresence) -> Unit) { presenceL.remove(l) }

    private fun setState(s: String) {
        state = s
    }

    @Synchronized
    fun retain(ctx: Context) {
        refs++
        if (socket == null) connect(ctx)
    }

    @Synchronized
    fun release() {
        refs = maxOf(0, refs - 1)
        if (refs == 0) {
            runCatching {
                socket?.disconnect()
                socket?.off()
            }
            socket = null
            setState(STATE_OFFLINE)
        }
    }

    fun isConnected(): Boolean = state == STATE_ONLINE && socket?.connected() == true

    // ─── Подключение ───

    private fun connect(ctx: Context) {
        val token = Store.token
        if (token.isNullOrEmpty()) {
            setState(STATE_OFFLINE)
            return
        }
        try {
            val opts = IO.Options().apply {
                transports = arrayOf(io.socket.engineio.client.transports.WebSocket.NAME)
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 2500
                reconnectionDelayMax = 8000
                timeout = 8000
                auth = mapOf("token" to token)
            }
            val s = IO.socket(URI(Store.apiBase), opts)
            socket = s

            s.on(Socket.EVENT_CONNECT) { setState(STATE_ONLINE) }
            s.on(Socket.EVENT_CONNECTING) { setState(STATE_CONNECTING) }
            s.on(Socket.EVENT_DISCONNECT) { setState(STATE_OFFLINE) }
            s.on(Socket.EVENT_CONNECT_ERROR) { setState(STATE_CONNECTING) }

            // ── message:new: полный Prisma-объект сообщения ──
            s.on("message:new", Emitter.Listener { args ->
                parseMessage(args.firstOrNull())?.let { m ->
                    messageNewL.toList().forEach { runCatching { it(m) } }
                }
            })

            // ── chat:updated: сервер прислал обновлённые метаданные чата ──
            s.on("chat:updated", Emitter.Listener { args ->
                parseChatUpdate(args.firstOrNull())?.let { u ->
                    chatUpdateL.toList().forEach { runCatching { it(u) } }
                }
            })

            // ── typing:update ──
            s.on("typing:update", Emitter.Listener { args ->
                val p = args.firstOrNull() as? JSONObject ?: return@Listener
                val t = RtTyping(
                    p.optString("chatId", ""),
                    p.optString("userId", ""),
                    p.optBoolean("isTyping", false)
                )
                if (t.chatId.isNotEmpty() && t.userId.isNotEmpty()) {
                    typingL.toList().forEach { runCatching { it(t) } }
                }
            })

            // ── presence:update (с учётом приватности: hidden=true если scope=NOBODY) ──
            s.on("presence:update", Emitter.Listener { args ->
                val p = args.firstOrNull() as? JSONObject ?: return@Listener
                val pr = RtPresence(
                    p.optString("userId", ""),
                    p.optString("status", "offline"),
                    p.optString("lastSeenAt").takeIf { it.isNotEmpty() && it != "null" },
                    p.optBoolean("hidden", false)
                )
                if (pr.userId.isNotEmpty()) {
                    presenceL.toList().forEach { runCatching { it(pr) } }
                }
            })

            setState(STATE_CONNECTING)
            s.connect()
        } catch (e: Exception) {
            setState(STATE_OFFLINE)
        }
    }

    // ─── Парсеры входящих событий ───

    private fun parseMessage(raw: Any?): RtMessage? {
        val payload = when (raw) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrNull()
            else -> null
        } ?: return null
        val id = payload.optString("id").takeIf { it.isNotEmpty() && it != "null" } ?: return null
        val chatId = payload.optString("chatId").takeIf { it.isNotEmpty() && it != "null" }
            ?: payload.optJSONObject("chat")?.optString("id")?.takeIf { it.isNotEmpty() }
            ?: return null
        val senderId = payload.optString("senderId").takeIf { it.isNotEmpty() && it != "null" }
            ?: payload.optJSONObject("sender")?.optString("id")?.takeIf { it.isNotEmpty() }
            ?: ""
        val senderName = payload.optJSONObject("sender")?.optString("username")?.takeIf { it.isNotEmpty() && it != "null" }
            ?: payload.optJSONObject("sender")?.optString("displayName")?.takeIf { it.isNotEmpty() && it != "null" }
            ?: "Кто-то"
        val text = payload.optString("text").takeIf { it.isNotEmpty() && it != "null" } ?: ""
        val chatTitle = payload.optJSONObject("chat")?.optString("title")?.takeIf { it.isNotEmpty() && it != "null" }
        val replyToId = payload.optString("replyToId").takeIf { it.isNotEmpty() && it != "null" }
        val attArr = payload.optJSONArray("attachments")
        val atts = mutableListOf<String>()
        if (attArr != null) {
            for (i in 0 until attArr.length()) {
                attArr.optJSONObject(i)?.optString("url")?.takeIf { it.isNotEmpty() && it != "null" }?.let { atts.add(it) }
            }
        }
        return RtMessage(id, chatId, senderId, senderName, text, chatTitle, replyToId, atts)
    }

    private fun parseChatUpdate(raw: Any?): RtChatUpdate? {
        val p = when (raw) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrNull()
            else -> null
        } ?: return null
        val id = p.optString("id").takeIf { it.isNotEmpty() && it != "null" } ?: return null
        val title = p.optString("title").takeIf { it.isNotEmpty() && it != "null" }
        val lm = p.optJSONObject("lastMessage")
        val lmText = lm?.optString("text")?.takeIf { it.isNotEmpty() && it != "null" }
        val lmAt = lm?.optString("createdAt")?.takeIf { it.isNotEmpty() && it != "null" }
            ?: p.optString("updatedAt").takeIf { it.isNotEmpty() && it != "null" }
        return RtChatUpdate(id, title, lmText, lmAt)
    }

    // ─── Исходящие сообщения ───

    /**
     * Отправка через сокет с ack.
     * onResult(ok, payload) — ok=true если сервер прислал созданный message (id не пустой).
     * Возвращает true, если emit прошёл (или в очереди); false — сокет недоступен, нужен HTTP-fallback.
     */
    fun sendMessage(
        chatId: String,
        text: String,
        replyToId: String?,
        onResult: (Boolean, RtMessage?) -> Unit
    ): Boolean {
        val s = socket ?: return false
        if (state == STATE_OFFLINE) return false
        val payload = JSONObject().apply {
            put("chatId", chatId)
            put("text", text)
            if (replyToId != null) put("replyToId", replyToId)
        }
        val ack = Ack { args ->
            val obj = args?.firstOrNull() as? JSONObject
            val parsed = obj?.let { parseMessage(it) }
            onResult(parsed != null, parsed)
        }
        s.emit("message:send", payload, ack)
        return true
    }

    fun editMessage(chatId: String, messageId: String, text: String, onResult: (Boolean) -> Unit = {}) {
        val s = socket ?: return
        val payload = JSONObject()
            .put("chatId", chatId)
            .put("messageId", messageId)
            .put("text", text)
        val ack = Ack { args ->
            val obj = args?.firstOrNull() as? JSONObject
            onResult(obj != null && obj.optString("id").isNotEmpty())
        }
        s.emit("message:edit", payload, ack)
    }

    fun deleteMessage(chatId: String, messageId: String, onResult: (Boolean) -> Unit = {}) {
        val s = socket ?: return
        val payload = JSONObject()
            .put("chatId", chatId)
            .put("messageId", messageId)
        val ack = Ack { args ->
            val obj = args?.firstOrNull() as? JSONObject
            onResult(obj?.optBoolean("ok") == true)
        }
        s.emit("message:delete", payload, ack)
    }

    fun reactToMessage(messageId: String, emoji: String, onResult: (Boolean) -> Unit = {}) {
        val s = socket ?: return
        val payload = JSONObject()
            .put("messageId", messageId)
            .put("emoji", emoji)
        val ack = Ack { args ->
            val obj = args?.firstOrNull() as? JSONObject
            onResult(obj != null)
        }
        s.emit("message:react", payload, ack)
    }

    // ─── Тайпинг ───

    fun startTyping(chatId: String) {
        val s = socket ?: return
        s.emit("typing:start", JSONObject().put("chatId", chatId))
    }

    fun stopTyping(chatId: String) {
        val s = socket ?: return
        s.emit("typing:stop", JSONObject().put("chatId", chatId))
    }

    // ─── Комнаты ───

    fun joinChat(chatId: String) {
        val s = socket ?: return
        s.emit("chat:join", JSONObject().put("chatId", chatId))
    }
}