package com.nexus.messenger.data

import android.content.Context
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import java.net.URI

data class RtMessage(
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val chatTitle: String?
)

data class RtPresence(
    val userId: String,
    val status: String,
    val hidden: Boolean
)

data class RtTyping(
    val chatId: String,
    val userId: String,
    val isTyping: Boolean
)

/**
 * Realtime-клиент Nexus под socket.io-гейтвей сервера.
 * Авторизация: handshake.auth.token (как в chat.gateway.ts).
 * События входа: message:new, message:updated, message:deleted,
 * message:reaction, typing:update, presence:update.
 * Исходящие: message:send (с ack), typing:start, typing:stop.
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
    private val chatDirtyL = mutableListOf<(String) -> Unit>()
    private val typingL = mutableListOf<(RtTyping) -> Unit>()
    private val presenceL = mutableListOf<(RtPresence) -> Unit>()

    fun addMessageListener(l: (RtMessage) -> Unit) { messageNewL.add(l) }
    fun removeMessageListener(l: (RtMessage) -> Unit) { messageNewL.remove(l) }
    fun addChatDirtyListener(l: (String) -> Unit) { chatDirtyL.add(l) }
    fun removeChatDirtyListener(l: (String) -> Unit) { chatDirtyL.remove(l) }
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
                // гейтвей читает handshake.auth.token
                auth = mapOf("token" to token)
            }
            val s = IO.socket(URI(Store.apiBase), opts)
            socket = s

            s.on(Socket.EVENT_CONNECT) { setState(STATE_ONLINE) }
            s.on(Socket.EVENT_CONNECTING) { setState(STATE_CONNECTING) }
            s.on(Socket.EVENT_DISCONNECT) { setState(STATE_OFFLINE) }
            s.on(Socket.EVENT_CONNECT_ERROR) { setState(STATE_CONNECTING) }

            s.on("message:new", Emitter.Listener { args ->
                parseMessage(args.firstOrNull())?.let { m ->
                    messageNewL.toList().forEach { runCatching { it(m) } }
                }
            })

            val dirty = Emitter.Listener { args ->
                val p = args.firstOrNull() as? JSONObject
                val chatId = p?.optString("chatId")?.takeIf { it.isNotEmpty() && it != "null" }
                    ?: (p?.optJSONObject("message")?.optString("chatId"))
                    ?: (p?.optJSONObject("chat")?.optString("id"))
                if (chatId != null) {
                    chatDirtyL.toList().forEach { runCatching { it(chatId) } }
                }
            }
            s.on("message:updated", dirty)
            s.on("message:deleted", dirty)
            s.on("message:reaction", dirty)

            s.on("typing:update", Emitter.Listener { args ->
                val p = args.firstOrNull() as? JSONObject ?: return@Listener
                val t = RtTyping(
                    p.optString("chatId"),
                    p.optString("userId"),
                    p.optBoolean("isTyping")
                )
                if (t.chatId.isNotEmpty()) {
                    typingL.toList().forEach { runCatching { it(t) } }
                }
            })

            s.on("presence:update", Emitter.Listener { args ->
                val p = args.firstOrNull() as? JSONObject ?: return@Listener
                val pr = RtPresence(
                    p.optString("userId"),
                    p.optString("status", "offline"),
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

    /** Отправка через сокет. false = сокет не готов, нужен HTTP-fallback */
    fun sendMessageViaSocket(
        chatId: String,
        text: String,
        replyToId: String?,
        onResult: (Boolean, JSONObject?) -> Unit
    ): Boolean {
        val s = socket ?: return false
        if (state != STATE_ONLINE) return false
        val payload = JSONObject().put("chatId", chatId).put("text", text)
        if (replyToId != null) payload.put("replyToId", replyToId)
        val ack = Ack { args ->
            val obj = args?.firstOrNull() as? JSONObject
            onResult(obj != null && obj.optString("id").isNotEmpty(), obj)
        }
        s.emit("message:send", payload, ack)
        return true
    }

    fun emitTyping(chatId: String, start: Boolean) {
        val s = socket ?: return
        if (state != STATE_ONLINE) return
        s.emit(
            if (start) "typing:start" else "typing:stop",
            JSONObject().put("chatId", chatId)
        )
    }

    /** Терпимый парсер message:new (полный Prisma-объект сообщения) */
    fun parseMessage(raw: Any?): RtMessage? {
        val payload = when (raw) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrNull()
            else -> null
        } ?: return null
        val msg = payload.optJSONObject("message") ?: payload
        val chatId = msg.optString("chatId").takeIf { it.isNotEmpty() && it != "null" }
            ?: msg.optJSONObject("chat")?.optString("id")?.takeIf { it.isNotEmpty() }
            ?: return null
        val senderId = msg.optString("senderId").takeIf { it.isNotEmpty() && it != "null" }
            ?: msg.optJSONObject("sender")?.optString("id")?.takeIf { it.isNotEmpty() }
            ?: msg.optString("authorId").takeIf { it.isNotEmpty() && it != "null" }
            ?: ""
        val senderName = msg.optJSONObject("sender")?.optString("username")?.takeIf { it.isNotEmpty() && it != "null" }
            ?: msg.optJSONObject("sender")?.optString("displayName")?.takeIf { it.isNotEmpty() && it != "null" }
            ?: msg.optString("authorName").takeIf { it.isNotEmpty() && it != "null" }
            ?: "Кто-то"
        val text = msg.optString("text").takeIf { it.isNotEmpty() && it != "null" } ?: ""
        val chatTitle = msg.optJSONObject("chat")?.optString("title")?.takeIf { it.isNotEmpty() && it != "null" }
        return RtMessage(chatId, senderId, senderName, text, chatTitle)
    }
}