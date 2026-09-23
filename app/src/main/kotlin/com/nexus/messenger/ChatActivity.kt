package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Message
import com.nexus.messenger.data.Store
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : Activity() {
    private lateinit var chatId: String
    private lateinit var titleText: TextView
    private lateinit var list: ListView
    private lateinit var input: EditText
    private lateinit var sendBtn: Button
    private lateinit var replyBar: LinearLayout
    private lateinit var replyText: TextView
    private val messages = mutableListOf<Message>()
    private var replyTo: Message? = null
    private var editing: Message? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getStringExtra("chatId") ?: return finish()
        val title = intent.getStringExtra("title") ?: "Чат"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(16, 24, 16, 24)
        }
        val back = Button(this).apply {
            text = "←"; setBackgroundColor(Color.TRANSPARENT)
            setTextColor(resources.getColor(R.color.accent, null)); textSize = 22f
        }
        back.setOnClickListener { finish() }
        header.addView(back)
        titleText = TextView(this).apply {
            text = title; textSize = 18f
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }
        header.addView(titleText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 16 })
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        replyBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; visibility = View.GONE
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(24, 12, 24, 12)
        }
        val replyContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val replyLabel = TextView(this).apply {
            text = "↩️ Ответ"; textSize = 12f
            setTextColor(resources.getColor(R.color.accent, null))
        }
        replyText = TextView(this).apply {
            textSize = 13f; setTextColor(resources.getColor(R.color.textSecondary, null))
            maxLines = 1
        }
        replyContent.addView(replyLabel)
        replyContent.addView(replyText)
        replyBar.addView(replyContent, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val closeReply = Button(this).apply {
            text = "✕"; setBackgroundColor(Color.TRANSPARENT)
            setTextColor(resources.getColor(R.color.textMuted, null))
        }
        closeReply.setOnClickListener { clearAction() }
        replyBar.addView(closeReply)
        root.addView(replyBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val adapter = object : ArrayAdapter<Message>(this@ChatActivity, 0, messages) {
            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                val msg = getItem(pos)!!
                val isOwn = msg.authorId == Store.user?.id
                val wrap = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 8, 0, 8)
                    gravity = if (isOwn) Gravity.END else Gravity.START
                }
                val bubble = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    val bg = GradientDrawable().apply {
                        setColor(resources.getColor(if (isOwn) R.color.messageOwn else R.color.messageOther, null))
                        cornerRadius = 40f
                    }
                    background = bg
                    setPadding(28, 16, 28, 16)
                }

                if (msg.replyText != null) {
                    val replyWrap = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(16, 4, 8, 8)
                    }
                    replyWrap.addView(TextView(context).apply {
                        text = msg.replyAuthor ?: "—"; textSize = 11f
                        setTextColor(resources.getColor(R.color.textPrimary, null))
                    })
                    replyWrap.addView(TextView(context).apply {
                        text = msg.replyText; textSize = 12f; maxLines = 2
                        setTextColor(resources.getColor(R.color.textSecondary, null))
                    })
                    val bar = View(context).apply {
                        setBackgroundColor(resources.getColor(R.color.textPrimary, null))
                    }
                    val h = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                    h.addView(bar, LinearLayout.LayoutParams(4, ViewGroup.LayoutParams.MATCH_PARENT))
                    h.addView(replyWrap)
                    bubble.addView(h)
                }

                bubble.addView(TextView(context).apply {
                    text = msg.text; textSize = 15f
                    setTextColor(resources.getColor(R.color.textPrimary, null))
                })

                val meta = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
                if (msg.updatedAt != null) {
                    meta.addView(TextView(context).apply {
                        text = "изм."; textSize = 10f
                        setTextColor(resources.getColor(R.color.textMuted, null))
                    })
                }
                meta.addView(TextView(context).apply {
                    text = formatTime(msg.createdAt); textSize = 10f
                    setTextColor(resources.getColor(R.color.textMuted, null))
                })
                bubble.addView(meta)

                wrap.addView(bubble)
                return wrap
            }
        }

        list = ListView(this).apply {
            this.adapter = adapter
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
            divider = null
        }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val inputBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(16, 12, 16, 12)
        }
        input = EditText(this).apply {
            hint = "Сообщение"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundColor(resources.getColor(R.color.bgInput, null))
            setPadding(24, 16, 24, 16)
        }
        inputBar.addView(input, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(0, 0, 12, 0)
        })
        sendBtn = Button(this).apply {
            text = "➤"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundColor(resources.getColor(R.color.accent, null))
        }
        sendBtn.setOnClickListener { send() }
        inputBar.addView(sendBtn, LinearLayout.LayoutParams(100, 100))
        root.addView(inputBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        setContentView(root)

        list.setOnItemLongClickListener { _, _, pos, _ ->
            val msg = adapter.getItem(pos) ?: return@setOnItemLongClickListener true
            val isOwn = msg.authorId == Store.user?.id
            val actions = mutableListOf("↩️ Ответить")
            if (isOwn) { actions.add("✏️ Редактировать"); actions.add("🗑️ Удалить") }

            AlertDialog.Builder(this)
                .setTitle("Действие")
                .setItems(actions.toTypedArray()) { _, i ->
                    when (actions[i]) {
                        "↩️ Ответить" -> {
                            replyTo = msg; editing = null
                            replyBar.visibility = View.VISIBLE
                            replyText.text = msg.text
                            input.hint = "Ответ..."
                        }
                        "✏️ Редактировать" -> {
                            editing = msg; replyTo = null
                            input.setText(msg.text)
                            input.hint = "Редактирование..."
                            sendBtn.text = "✓"
                        }
                        "🗑️ Удалить" -> {
                            AlertDialog.Builder(this)
                                .setMessage("Удалить сообщение?")
                                .setPositiveButton("Удалить") { _, _ ->
                                    Api.delete("/messages/${msg.id}") { _, _ -> loadMessages() }
                                }
                                .setNegativeButton("Отмена", null)
                                .show()
                        }
                    }
                }
                .show()
            true
        }

        loadMessages()
    }

    private fun clearAction() {
        replyTo = null; editing = null
        replyBar.visibility = View.GONE
        input.setText(""); input.hint = "Сообщение"
        sendBtn.text = "➤"
    }

    private fun send() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return

        if (editing != null) {
            Api.patch("/messages/${editing!!.id}", JSONObject().put("text", text)) { _, _ ->
                runOnUiThread { clearAction(); loadMessages() }
            }
        } else {
            val body = JSONObject().put("text", text)
            replyTo?.let { body.put("replyToId", it.id) }
            Api.post("/chats/$chatId/messages", body) { _, _ ->
                runOnUiThread { clearAction(); loadMessages() }
            }
        }
    }

    private fun loadMessages() {
        Api.get("/chats/$chatId/messages") { code, body ->
            runOnUiThread {
                messages.clear()
                if (code == 200) {
                    val arr = Api.parseArray(body)
                    for (i in 0 until arr.length()) messages.add(Message.fromJson(arr.getJSONObject(i)))
                    @Suppress("UNCHECKED_CAST")
                    (list.adapter as ArrayAdapter<Message>).notifyDataSetChanged()
                    list.setSelection(messages.size - 1)
                }
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