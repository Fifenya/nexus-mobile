package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Chat
import com.nexus.messenger.ui.dp
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatsActivity : Activity() {
    private lateinit var listView: ListView
    private lateinit var emptyText: TextView
    private val chats = mutableListOf<Chat>()
    private lateinit var chatAdapter: ArrayAdapter<Chat>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        }

        // ─── Шапка ───
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
            setPadding(dp(20), dp(16), dp(12), dp(12))
        }
        header.addView(TextView(this).apply {
            text = "Чаты"
            textSize = 28f
            setTextColor(resources.getColor(R.color.textPrimary, null))
            paintFlags = paintFlags or android.graphics.Paint.ANTI_ALIAS_FLAG
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

        val settingsBtn = TextView(this).apply {
            text = "⚙"
            textSize = 22f
            setTextColor(resources.getColor(R.color.textSecondary, null))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        settingsBtn.setOnClickListener {
            startActivity(Intent(this@ChatsActivity, SettingsActivity::class.java))
        }
        header.addView(settingsBtn)
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // ─── Поиск ───
        val searchInput = EditText(this).apply {
            hint = "🔍 Поиск"
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundResource(R.drawable.bg_search)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            textSize = 15f
            singleLine = true
        }
        root.addView(searchInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(dp(16), dp(4), dp(16), dp(8))
        })

        // ─── Адаптер ───
        chatAdapter = object : ArrayAdapter<Chat>(this@ChatsActivity, 0, chats) {
            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                val chat = getItem(pos)!!

                val view = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    setBackgroundResource(R.drawable.bg_chat_item)
                }

                // Аватарка (круглая)
                val avatar = TextView(context).apply {
                    text = (chat.title ?: "Ч").take(1).uppercase()
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(resources.getColor(R.color.textPrimary, null))
                    setBackgroundResource(R.drawable.bg_avatar)
                }
                view.addView(avatar, LinearLayout.LayoutParams(dp(54), dp(54)))

                // Контент
                val content = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                }

                // Строка 1: название + время
                val row1 = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row1.addView(TextView(context).apply {
                    text = (if (chat.pinned) "📌 " else "") + (chat.title ?: "Чат")
                    textSize = 16f
                    setTextColor(resources.getColor(R.color.textPrimary, null))
                    maxLines = 1
                }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                row1.addView(TextView(context).apply {
                    text = formatTime(chat.lastMessageAt)
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.textMuted, null))
                })
                content.addView(row1)

                // Строка 2: превью + бейдж
                val row2 = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row2.addView(TextView(context).apply {
                    text = chat.lastMessage ?: "Нет сообщений"
                    textSize = 14f
                    setTextColor(resources.getColor(R.color.textSecondary, null))
                    maxLines = 1
                }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                if (chat.unreadCount > 0) {
                    row2.addView(TextView(context).apply {
                        text = "${chat.unreadCount}"
                        textSize = 12f
                        gravity = Gravity.CENTER
                        setTextColor(resources.getColor(R.color.textPrimary, null))
                        setBackgroundResource(R.drawable.bg_badge)
                        minWidth = dp(22)
                        setPadding(dp(7), dp(2), dp(7), dp(2))
                    })
                }
                content.addView(row2, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = dp(3)
                })

                view.addView(content, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(14)
                })

                return view
            }
        }

        listView = ListView(this).apply {
            adapter = chatAdapter
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
            divider = null
            dividerHeight = 0
            clipToPadding = false
            setPadding(0, 0, 0, dp(80))
        }
        root.addView(listView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        // ─── Empty ───
        emptyText = TextView(this).apply {
            text = "Нет чатов\nСоздайте первый чат"
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(resources.getColor(R.color.textMuted, null))
            visibility = View.GONE
        }
        root.addView(emptyText, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        // ─── FAB ───
        val frame = FrameLayout(this)
        frame.addView(root, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val fab = TextView(this).apply {
            text = "✏"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundResource(R.drawable.bg_fab)
            elevation = dp(6).toFloat()
        }
        fab.setOnClickListener { createChat() }
        frame.addView(fab, FrameLayout.LayoutParams(dp(56), dp(56)).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(0, 0, dp(20), dp(20))
        })

        setContentView(frame)

        // ─── Клики ───
        listView.setOnItemClickListener { _, _, pos, _ ->
            val chat = chatAdapter.getItem(pos) ?: return@setOnItemClickListener
            startActivity(Intent(this, ChatActivity::class.java)
                .putExtra("chatId", chat.id)
                .putExtra("title", chat.title))
        }

        listView.setOnItemLongClickListener { _, _, pos, _ ->
            val chat = chatAdapter.getItem(pos) ?: return@setOnItemLongClickListener true
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(chat.title ?: "Чат")
                .setItems(arrayOf(if (chat.pinned) "📌 Открепить" else "📌 Закрепить")) { _, i ->
                    val path = if (i == 0 && chat.pinned) "/chats/${chat.id}/unpin" else "/chats/${chat.id}/pin"
                    Api.post(path, JSONObject()) { _, _ -> loadChats() }
                }
                .show()
            true
        }

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                chatAdapter.filter.filter(s)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadChats()
    }

    private fun loadChats() {
        Api.get("/chats") { code, body ->
            runOnUiThread {
                chats.clear()
                if (code == 200) {
                    val arr = Api.parseArray(body)
                    val temp = mutableListOf<Chat>()
                    for (i in 0 until arr.length()) temp.add(Chat.fromJson(arr.getJSONObject(i)))
                    temp.sortWith(compareByDescending<Chat> { it.pinned })
                    chats.addAll(temp)
                    chatAdapter.notifyDataSetChanged()
                }
                emptyText.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
                listView.visibility = if (chats.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun createChat() {
        val input = EditText(this).apply {
            hint = "Название чата"
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Новый чат")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val title = input.text.toString().trim()
                Api.post("/chats", JSONObject().put("title", title).put("userIds", org.json.JSONArray())) { code, body ->
                    runOnUiThread {
                        if (code == 200 || code == 201) loadChats()
                        else AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                            .setMessage(body).setPositiveButton("OK", null).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun formatTime(s: String?): String {
        if (s.isNullOrEmpty()) return ""
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(s) ?: return ""
            val diff = (System.currentTimeMillis() - date.time) / 86400000L
            when {
                diff == 0L -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                diff == 1L -> "Вчера"
                diff < 7 -> SimpleDateFormat("EEE", Locale("ru")).format(date)
                else -> SimpleDateFormat("dd.MM", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) { "" }
    }
}