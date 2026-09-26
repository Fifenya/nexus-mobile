package com.nexus.messenger

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Cache
import com.nexus.messenger.data.Message
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.User
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class ChatActivity : Activity() {
    private lateinit var chatId: String
    private lateinit var listView: ListView
    private lateinit var input: EditText
    private lateinit var sendBtn: ImageView
    private lateinit var replyBar: LinearLayout
    private lateinit var replyLabel: TextView
    private lateinit var replyPreview: TextView
    private lateinit var statusTv: TextView
    private val messages = mutableListOf<Message>()
    private lateinit var msgAdapter: ArrayAdapter<Message>
    private var replyTo: Message? = null
    private var editing: Message? = null
    private var isGroup = false
    private var otherUserId: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val presenceTask = object : Runnable {
        override fun run() {
            refreshPresence()
            handler.postDelayed(this, 20000)
        }
    }

    private val avatarColors = intArrayOf(
        0xFFE17076.toInt(), 0xFF7BC862.toInt(), 0xFF65AADD.toInt(),
        0xFFA695E7.toInt(), 0xFFEE7AAE.toInt(), 0xFF6EC9CB.toInt(), 0xFFFAA774.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getStringExtra("chatId") ?: run { finish(); return }
        val title = intent.getStringExtra("title") ?: "Чат"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }

        // ── Шапка ──
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(color(R.color.bgSecondary))
            setPadding(dp(4), dp(8), dp(8), dp(8))
        }
        val back = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            imageTintList = ColorStateList.valueOf(color(R.color.accentText))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        back.setOnClickListener { finish() }
        header.addView(back, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        val avatar = TextView(this).apply {
            text = title.take(1).uppercase()
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.tileCircle(this@ChatActivity, avatarColors[Math.abs(title.hashCode()) % avatarColors.size])
        }
        header.addView(avatar, LinearLayout.LayoutParams(dp(42), dp(42)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        info.addView(Ui.text(this, title, 17f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        statusTv = Ui.text(this, "не в сети", 12f, R.color.textMuted)
        info.addView(statusTv, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        header.addView(info, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(10) })
        val dots = ImageView(this).apply {
            setImageResource(R.drawable.ic_dots)
            imageTintList = ColorStateList.valueOf(color(R.color.textSecondary))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        dots.setOnClickListener {
            NxDialog(this).title(title).items(listOf("Обновить", "Очистить кэш чата")) { i ->
                when (i) {
                    0 -> loadMessages()
                    1 -> {
                        Cache.put(this, "msgs_$chatId", "[]")
                        Ui.snackbar(this, "Кэш чата очищен")
                    }
                }
            }.show()
        }
        header.addView(dots, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // ── Панель ответа/редактирования ──
        replyBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            background = Ui.card(this@ChatActivity)
            setPadding(dp(12), dp(8), dp(8), dp(8))
        }
        val replyMid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        replyLabel = Ui.text(this, "", 12f, R.color.accentText, true)
        replyPreview = Ui.text(this, "", 13f, R.color.textSecondary)
        replyPreview.maxLines = 1
        replyMid.addView(replyLabel, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        replyMid.addView(replyPreview, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        replyBar.addView(replyMid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val closeReply = ImageView(this).apply {
            setImageResource(R.drawable.ic_close)
            imageTintList = ColorStateList.valueOf(color(R.color.textMuted))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        closeReply.setOnClickListener { clearAction() }
        replyBar.addView(closeReply, LinearLayout.LayoutParams(dp(36), dp(36)))
        root.addView(replyBar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            leftMargin = dp(10); rightMargin = dp(10); topMargin = dp(6)
        })

        // ── Адаптер сообщений ──
        msgAdapter = object : ArrayAdapter<Message>(this@ChatActivity, 0, messages) {
            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                val msg = getItem(pos)!!
                val isOwn = msg.authorId == Store.user?.id
                val wrap = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dp(40), dp(3), dp(40), dp(3))
                    gravity = if (isOwn) Gravity.END else Gravity.START
                }
                val r = dp(16).toFloat()
                val t = dp(4).toFloat()
                val bubble = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    background = GradientDrawable().apply {
                        setColor(context.resources.getColor(if (isOwn) R.color.messageOwn else R.color.messageOther, null))
                        cornerRadii = if (isOwn)
                            floatArrayOf(r, r, r, r, t, t, r, r)
                        else
                            floatArrayOf(r, r, r, r, r, r, t, t)
                    }
                    setPadding(dp(12), dp(8), dp(12), dp(6))
                }

                if (!isOwn && isGroup) {
                    bubble.addView(Ui.text(context, msg.authorName, 12f, R.color.accentText, true),
                        LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { bottomMargin = dp(2) })
                }

                if (msg.replyText != null) {
                    val quote = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                    quote.addView(View(context).apply {
                        setBackgroundColor(context.resources.getColor(R.color.accentText, null))
                    }, LinearLayout.LayoutParams(dp(3), MATCH_PARENT))
                    val qMid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
                    qMid.addView(Ui.text(context, msg.replyAuthor ?: "", 11f, R.color.accentText, true),
                        LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
                    val qText = Ui.text(context, msg.replyText ?: "", 12f, R.color.textSecondary)
                    qText.maxLines = 2
                    qMid.addView(qText, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
                    quote.addView(qMid, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dp(8) })
                    bubble.addView(quote, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { bottomMargin = dp(6) })
                }

                if (msg.text.isNotEmpty()) {
                    bubble.addView(Ui.text(context, msg.text, 15f, R.color.textPrimary),
                        LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
                } else {
                    bubble.addView(Ui.text(context, "📎 Вложение", 14f, R.color.textSecondary),
                        LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
                }

                val meta = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                }
                val metaColor = if (isOwn) 0xB3FFFFFF.toInt() else color(R.color.textMuted)
                if (msg.updatedAt != null) {
                    meta.addView(TextView(context).apply {
                        text = "изм. "
                        textSize = 10f
                        setTextColor(metaColor)
                    })
                }
                meta.addView(TextView(context).apply {
                    text = formatTime(msg.createdAt)
                    textSize = 10f
                    setTextColor(metaColor)
                })
                if (isOwn) {
                    val myId = Store.user?.id
                    val read = msg.viewIds.any { it != myId && it.isNotEmpty() }
                    meta.addView(ImageView(context).apply {
                        setImageResource(if (read) R.drawable.ic_dcheck else R.drawable.ic_check)
                        imageTintList = ColorStateList.valueOf(metaColor)
                    }, LinearLayout.LayoutParams(dp(14), dp(14)).apply { leftMargin = dp(4) })
                }
                bubble.addView(meta, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })

                wrap.addView(bubble, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
                return wrap
            }
        }

        listView = ListView(this).apply {
            adapter = msgAdapter
            setBackgroundColor(color(R.color.bgPrimary))
            divider = null
            dividerHeight = 0
            transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
        }
        root.addView(listView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        // ── Поле ввода ──
        val inputBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(color(R.color.bgSecondary))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val attach = ImageView(this).apply {
            setImageResource(R.drawable.ic_attach)
            imageTintList = ColorStateList.valueOf(color(R.color.textSecondary))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        attach.setOnClickListener { Ui.snackbar(this, "Вложения появятся позже") }
        inputBar.addView(attach, LinearLayout.LayoutParams(dp(40), dp(40)))
        input = EditText(this).apply {
            hint = "Сообщение"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = Ui.pill(this@ChatActivity, R.color.bgInput)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            textSize = 15f
        }
        inputBar.addView(input, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            leftMargin = dp(6); rightMargin = dp(8)
        })
        sendBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_send)
            imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
            background = Ui.pill(this@ChatActivity, R.color.accent)
            setPadding(dp(11), dp(11), dp(11), dp(11))
            isClickable = true
        }
        sendBtn.setOnClickListener { send() }
        inputBar.addView(sendBtn, LinearLayout.LayoutParams(dp(46), dp(46)))
        root.addView(inputBar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        setContentView(root)

        listView.setOnItemLongClickListener { _, _, pos, _ ->
            val msg = msgAdapter.getItem(pos) ?: return@setOnItemLongClickListener true
            val isOwn = msg.authorId == Store.user?.id
            val actions = mutableListOf("Ответить")
            if (isOwn) { actions.add("Редактировать"); actions.add("Удалить") }
            NxDialog(this).items(actions) { i ->
                when (actions[i]) {
                    "Ответить" -> {
                        replyTo = msg; editing = null
                        replyBar.visibility = View.VISIBLE
                        replyLabel.text = "↩ ${msg.authorName}"
                        replyPreview.text = msg.text
                    }
                    "Редактировать" -> {
                        editing = msg; replyTo = null
                        input.setText(msg.text)
                        replyBar.visibility = View.VISIBLE
                        replyLabel.text = "✎ Редактирование"
                        replyPreview.text = msg.text
                    }
                    "Удалить" -> {
                        NxDialog(this)
                            .message("Удалить сообщение?")
                            .button("Удалить") {
                                Api.delete("/messages/${msg.id}") { _, _ -> loadMessages() }
                            }
                            .button("Отмена") {}
                            .show()
                    }
                }
            }.show()
            true
        }

        // Сначала кэш (мгновенно), потом сеть
        loadFromCache()
        loadMessages()
        handler.post(presenceTask)
    }

    override fun onResume() {
        super.onResume()
        markViewed()
    }

    override fun onDestroy() {
        handler.removeCallbacks(presenceTask)
        super.onDestroy()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun clearAction() {
        replyTo = null; editing = null
        replyBar.visibility = View.GONE
        input.setText("")
        input.hint = "Сообщение"
    }

    private fun send() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) {
            Ui.snackbar(this, "Пустое сообщение")
            return
        }
        sendBtn.isEnabled = false
        if (editing != null) {
            Api.patch("/messages/${editing!!.id}", JSONObject().put("text", text)) { code, body ->
                runOnUiThread {
                    sendBtn.isEnabled = true
                    if (code in 200..299) {
                        clearAction(); loadMessages()
                    } else {
                        Ui.snackbar(this, Api.friendlyError(body))
                    }
                }
            }
        } else {
            val body = JSONObject().put("text", text)
            replyTo?.let { body.put("replyToId", it.id) }
            Api.post("/chats/$chatId/messages", body) { code, resp ->
                runOnUiThread {
                    sendBtn.isEnabled = true
                    if (code in 200..299) {
                        clearAction(); loadMessages()
                    } else {
                        Ui.snackbar(this, "Не отправлено: ${Api.friendlyError(resp)}")
                    }
                }
            }
        }
    }

    /** Сообщаем серверу, что мы прочитали чужие сообщения (галочки у собеседника) */
    private fun markViewed() {
        val myId = Store.user?.id
        val ids = messages.filter { it.authorId != myId && it.authorId.isNotEmpty() }.map { it.id }
        if (ids.isEmpty()) return
        Api.post("/view", JSONObject().put("messageIds", JSONArray(ids))) { _, _ -> }
    }

    private fun loadFromCache() {
        val raw = Cache.get(this, "msgs_$chatId") ?: return
        renderMessages(parseMessages(raw))
    }

    private fun loadMessages() {
        Api.get("/chats/$chatId/messages") { code, body ->
            runOnUiThread {
                if (code == 200) {
                    Cache.put(this, "msgs_$chatId", body)
                    renderMessages(parseMessages(body))
                    markViewed()
                } else if (code != 0) {
                    Ui.snackbar(this, Api.friendlyError(body))
                }
            }
        }
    }

    private fun renderMessages(list: List<Message>) {
        messages.clear()
        messages.addAll(list)
        isGroup = messages.distinctBy { it.authorId }.size > 2
        otherUserId = messages.firstOrNull { it.authorId != Store.user?.id && it.authorId.isNotEmpty() }?.authorId
        msgAdapter.notifyDataSetChanged()
        if (messages.isNotEmpty()) listView.setSelection(messages.size - 1)
        refreshPresence()
    }

    /** Оба формата ответа: массив или {items: [...]} */
    private fun parseMessages(body: String): List<Message> {
        val trimmed = body.trim()
        val arr = if (trimmed.startsWith("[")) {
            Api.parseArray(body)
        } else {
            val o = Api.parseObj(body)
            o?.optJSONArray("items") ?: o?.optJSONArray("messages") ?: JSONArray()
        }
        val out = mutableListOf<Message>()
        for (i in 0 until arr.length()) out.add(Message.fromJson(arr.getJSONObject(i)))
        return out
    }

    private fun refreshPresence() {
        val oid = otherUserId ?: return
        if (isGroup) {
            statusTv.text = "группа"
            statusTv.setTextColor(color(R.color.textMuted))
            return
        }
        Api.get("/users/$oid") { code, body ->
            runOnUiThread {
                if (code != 200) return@runOnUiThread
                val u = User.fromJson(Api.parseObj(body) ?: return@runOnUiThread)
                val online = u.onlineStatus == "online" || u.online
                statusTv.text = if (online) "в сети" else "не в сети"
                statusTv.setTextColor(color(if (online) R.color.online else R.color.textMuted))
            }
        }
    }

    private fun formatTime(s: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(s) ?: return ""
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        } catch (e: Exception) { "" }
    }
}