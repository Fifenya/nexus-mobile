package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.Chat
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ChatsActivity : Activity() {
    private lateinit var list: ListView
    private lateinit var searchInput: EditText
    private lateinit var emptyText: TextView
    private val chats = mutableListOf<Chat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(32, 32, 32, 32)
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Чаты"; textSize = 22f; setTextColor(resources.getColor(R.color.textPrimary, null))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val menuBtn = Button(this).apply {
            text = "⚙"; setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }
        menuBtn.setOnClickListener { startActivity(Intent(this@ChatsActivity, SettingsActivity::class.java)) }
        header.addView(menuBtn)
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        searchInput = EditText(this).apply {
            hint = "Поиск"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundColor(resources.getColor(R.color.bgInput, null))
            setPadding(32, 20, 32, 20)
        }
        root.addView(searchInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(16, 16, 16, 16)
        })

        val adapter = object : ArrayAdapter<Chat>(this@ChatsActivity, 0, chats) {
            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                val chat = getItem(pos)!!
                val view = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL; setPadding(32, 24, 32, 24)
                    setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
                }

                val avatar = TextView(context).apply {
                    text = (chat.title ?: "Ч").take(1).uppercase()
                    textSize = 22f; gravity = Gravity.CENTER
                    setTextColor(resources.getColor(R.color.textPrimary, null))
                    setBackgroundColor(resources.getColor(R.color.accent, null))
                }
                view.addView(avatar, LinearLayout.LayoutParams(120, 120))

                val content = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                }
                val titleRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                val title = TextView(context).apply {
                    text = (if (chat.pinned) "📌 " else "") + (chat.title ?: "Чат")
                    textSize = 16f; setTextColor(resources.getColor(R.color.textPrimary, null))
                }
                titleRow.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                titleRow.addView(TextView(context).apply {
                    text = formatTime(chat.lastMessageAt); textSize = 12f
                    setTextColor(resources.getColor(R.color.textMuted, null))
                })
                content.addView(titleRow)
                val previewRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
                previewRow.addView(TextView(context).apply {
                    text = chat.lastMessage ?: "Нет сообщений"; textSize = 14f
                    setTextColor(resources.getColor(R.color.textSecondary, null))
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                if (chat.unreadCount > 0) {
                    previewRow.addView(TextView(context).apply {
                        text = " ${chat.unreadCount} "; textSize = 12f
                        setTextColor(resources.getColor(R.color.textPrimary, null))
                        setBackgroundColor(resources.getColor(R.color.accent, null))
                        setPadding(12, 4, 12, 4)
                    })
                }
                content.addView(previewRow)
                view.addView(content, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 24 })

                return view
            }
        }

        list = ListView(this)
        list.adapter = adapter
        list.setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        list.divider = android.graphics.drawable.ColorDrawable(resources.getColor(R.color.border, null))
        list.dividerHeight = 1
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        emptyText = TextView(this).apply {
            text = "Нет чатов"; gravity = Gravity.CENTER; textSize = 16f
            setTextColor(resources.getColor(R.color.textSecondary, null))
            visibility = View.GONE
        }
        root.addView(emptyText)

        val frame = FrameLayout(this)
        frame.addView(root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val fab = Button(this).apply {
            text = "✏"; textSize = 24f
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundColor(resources.getColor(R.color.accent, null))
        }
        fab.setOnClickListener { createChat() }
        val fabLp = FrameLayout.LayoutParams(140, 140).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(0, 0, 48, 48)
        }
        frame.addView(fab, fabLp)

        setContentView(frame)

        list.setOnItemClickListener { _, _, pos, _ ->
            val chat = adapter.getItem(pos) ?: return@setOnItemClickListener
            startActivity(Intent(this, ChatActivity::class.java).putExtra("chatId", chat.id).putExtra("title", chat.title))
        }

        list.setOnItemLongClickListener { _, _, pos, _ ->
            val chat = adapter.getItem(pos) ?: return@setOnItemLongClickListener true
            AlertDialog.Builder(this)
                .setTitle(chat.title ?: "Чат")
                .setItems(arrayOf(if (chat.pinned) "Открепить" else "Закрепить")) { _, i ->
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
                @Suppress("UNCHECKED_CAST")
                (list.adapter as ArrayAdapter<Chat>).filter.filter(s)
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
                    val list = mutableListOf<Chat>()
                    for (i in 0 until arr.length()) list.add(Chat.fromJson(arr.getJSONObject(i)))
                    list.sortWith(compareByDescending<Chat> { it.pinned })
                    chats.addAll(list)
                    @Suppress("UNCHECKED_CAST")
                    (list.adapter as ArrayAdapter<Chat>).notifyDataSetChanged()
                }
                emptyText.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
                list.visibility = if (chats.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun createChat() {
        val input = EditText(this).apply {
            hint = "Название нового чата"
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
        }
        AlertDialog.Builder(this)
            .setTitle("Новый чат")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val title = input.text.toString().trim()
                Api.post("/chats", JSONObject().put("title", title).put("userIds", org.json.JSONArray())) { code, body ->
                    runOnUiThread {
                        if (code == 200 || code == 201) {
                            loadChats()
                        } else {
                            AlertDialog.Builder(this).setMessage(body).setPositiveButton("OK", null).show()
                        }
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
            val now = System.currentTimeMillis()
            val diff = (now - date.time) / 86400000L
            when {
                diff == 0L -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                diff == 1L -> "Вчера"
                diff < 7 -> SimpleDateFormat("EEE", Locale("ru")).format(date)
                else -> SimpleDateFormat("dd.MM", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) { "" }
    }
}