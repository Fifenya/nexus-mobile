package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Message
import com.nexus.messenger.data.Store
import com.nexus.messenger.ui.dp
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : Activity() {
    private lateinit var chatId: String
    private lateinit var listView: ListView
    private lateinit var input: EditText
    private lateinit var sendBtn: TextView
    private lateinit var replyBar: LinearLayout
    private lateinit var replyPreview: TextView
    private lateinit var replyLabel: TextView
    private val messages = mutableListOf<Message>()
    private var replyTo: Message? = null
    private var editing: Message? = null
    private lateinit var msgAdapter: ArrayAdapter<Message>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getStringExtra("chatId") ?: return finish()
        val title = intent.getStringExtra("title") ?: "Чат"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        }

        // ─── Шапка ───
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(dp(8), dp(10), dp(12), dp(10))
        }

        val backBtn = TextView(this).apply {
            text = "←"
            textSize = 24f
            setTextColor(resources.getColor(R.color.accent, null))
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }
        backBtn.setOnClickListener { finish() }
        header.addView(backBtn)

        // Аватарка в шапке
        val headerAvatar = TextView(this).apply {
            text = title.take(1).uppercase()
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundResource(R.drawable.bg_avatar)
        }
        header.addView(headerAvatar, LinearLayout.LayoutParams(dp(40), dp(40)))

        val headerInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        headerInfo.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTextColor(resources.getColor(R.color.textPrimary, null))
            maxLines = 1
        })
        headerInfo.addView(TextView(this).apply {
            text = "в сети"
            textSize = 12f
            setTextColor(resources.getColor(R.color.online, null))
        })
        header.addView(headerInfo, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            leftMargin = dp(10)
        })

        val menuBtn = TextView(this).apply {
            text = "⋮"
            textSize = 22f
            setTextColor(resources.getColor(R.color.textSecondary, null))
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        header.addView(menuBtn)
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // ─── Reply bar ───
        replyBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setBackgroundResource(R.drawable.bg_reply_bar)
            setPadding(dp(16), dp(8), dp(8), dp(8))
        }
        val replyContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        replyLabel = TextView(this).apply {
            textSize = 12f
            setTextColor(resources.getColor(R.color.accent, null))
        }
        replyPreview = TextView(this).apply {
            textSize = 13f
            setTextColor(resources.getColor(R.color.textSecondary, null))
            maxLines = 1
        }
        replyContent.addView(replyLabel)
        replyContent.addView(replyPreview)
        replyBar.addView(replyContent, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val closeReply = TextView(this).apply {
            text = "✕"
            textSize = 18f
            setTextColor(resources.getColor(R.color.textMuted, null))
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }
        closeReply.setOnClickListener { clearAction() }
        replyBar.addView(closeReply)
        root.addView(replyBar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(dp(12), dp(6), dp(12), dp(2))
        })

        // ─── Адаптер сообщений ───
        msgAdapter = object : ArrayAdapter<Message>(this@ChatActivity, 0, messages) {
            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                val msg = getItem(pos)!!
                val isOwn = msg.authorId == Store.user?.id

                val wrap = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(dp(12), dp(3), dp(12), dp(3))
                    gravity = if (isOwn) Gravity.END else Gravity.START
                }

                val bubble = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundResource(
                        if (isOwn) R.drawable.bg_bubble_own else R.drawable.bg_bubble_other
                    )
                    setPadding(dp(14), dp(10), dp(14), dp(8))
                    maxWidth = dp(280)
                }

                // Имя автора (для чужих в группах)
                if (!isOwn) {
                    bubble.addView(TextView(context).apply {
                        text = msg.authorName
                        textSize = 12f
                        setTextColor(resources.getColor(R.color.accent, null))
                    }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        bottomMargin = dp(2)
                    })
                }

                // Reply внутри пузыря
                if (msg.replyText != null) {
                    val replyBox = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }
                    val bar = View(context).apply {
                        setBackgroundColor(resources.getColor(R.color.textPrimary, null))
                    }
                    replyBox.addView(bar, LinearLayout.LayoutParams(dp(3), MATCH_PARENT))
                    val replyInfo = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                    }
                    replyInfo.addView(TextView(context).apply {
                        text = msg.replyAuthor ?: ""
                        textSize = 11f
                        setTextColor(resources.getColor(R.color.textPrimary, null))
                    })
                    replyInfo.addView(TextView(context).apply {
                        text = msg.replyText
                        textSize = 12f
                        maxLines = 2
                        setTextColor(resources.getColor(R.color.textSecondary, null))
                    })
                    replyBox.addView(replyInfo, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        leftMargin = dp(8)
                    })
                    bubble.addView(replyBox, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        bottomMargin = dp(6)
                    })
                }

                // Текст сообщения
                bubble.addView(TextView(context).apply {
                    text = msg.text
                    textSize = 15f
                    setTextColor(resources.getColor(R.color.textPrimary, null))
                })

                // Мета (время + edited)
                val meta = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                }
                if (msg.updatedAt != null) {
                    meta.addView(TextView(context).apply {
                        text = "изм. · "
                        textSize = 10f
                        setTextColor(
                            if (isOwn) Color.parseColor("#99ffffff")
                            else resources.getColor(R.color.textMuted, null)
                        )
                    })
                }
                meta.addView(TextView(context).apply {
                    text = formatTime(msg.createdAt)
                    textSize = 10f
                    setTextColor(
                        if (isOwn) Color.parseColor("#99ffffff")
                        else resources.getColor(R.color.textMuted, null)
                    )
                })
                bubble.addView(meta, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = dp(2)
                })

                wrap.addView(bubble)
                return wrap
            }
        }

        listView = ListView(this).apply {
            adapter = msgAdapter
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
            divider = null
            dividerHeight = 0
            stackFromBottom = true
        }
        root.addView(listView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        // ─── Поле ввода ───
        val inputBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }

        val attachBtn = TextView(this).apply {
            text = "📎"
            textSize = 22f
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        inputBar.addView(attachBtn)

        input = EditText(this).apply {
            hint = "Сообщение"
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundResource(R.drawable.bg_search)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            textSize = 15f
        }
        inputBar.addView(input, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            setMargins(dp(6), 0, dp(8), 0)
        })

        sendBtn = TextView(this).apply {
            text = "➤"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundResource(R.drawable.bg_fab)
        }
        sendBtn.setOnClickListener { send() }
        inputBar.addView(sendBtn, LinearLayout.LayoutParams(dp(44), dp(44)))

        root.addView(inputBar, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        setContentView(root)

        // ─── Долгое нажатие на сообщение ───
        listView.setOnItemLongClickListener { _, _, pos, _ ->
            val msg = msgAdapter.getItem(pos) ?: return@setOnItemLongClickListener true
            val isOwn = msg.authorId == Store.user?.id
            val actions = mutableListOf("↩️ Ответить")
            if (isOwn) { actions.add("✏️ Редактировать"); actions.add("🗑️ Удалить") }

            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setItems(actions.toTypedArray()) { _, i ->
                    when (actions[i]) {
                        "↩️ Ответить" -> {
                            replyTo = msg; editing = null
                            replyBar.visibility = View.VISIBLE
                            replyLabel.text = "↩️ ${msg.authorName}"
                            replyPreview.text = msg.text
                        }
                        "✏️ Редактировать" -> {
                            editing = msg; replyTo = null
                            input.setText(msg.text)
                            sendBtn.text = "✓"
                            replyBar.visibility = View.VISIBLE
                            replyLabel.text = "✏️ Редактирование"
                            replyPreview.text = msg.text
                        }
                        "🗑️ Удалить" -> {
                            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
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
                    msgAdapter.notifyDataSetChanged()
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