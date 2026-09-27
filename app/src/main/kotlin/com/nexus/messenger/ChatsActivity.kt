package com.nexus.messenger

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
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
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Cache
import com.nexus.messenger.data.Chat
import com.nexus.messenger.data.ChatFolder
import com.nexus.messenger.data.FoldersStore
import com.nexus.messenger.data.LocalPrefs
import com.nexus.messenger.data.Notify
import com.nexus.messenger.data.RealtimeClient
import com.nexus.messenger.data.RtMessage
import com.nexus.messenger.data.Store
import com.nexus.messenger.ui.BottomNav
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class ChatsActivity : Activity() {
    private lateinit var listView: ListView
    private lateinit var emptyText: TextView
    private lateinit var chipsBar: LinearLayout
    private lateinit var titleTv: TextView
    private val chats = mutableListOf<Chat>()
    private val allChatsList = mutableListOf<Chat>()
    private lateinit var chatAdapter: ArrayAdapter<Chat>
    private var currentFolder: ChatFolder? = null

    private val dotsHandler = Handler(Looper.getMainLooper())
    private var dotsCount = 0
    private val dotsTask = object : Runnable {
        override fun run() {
            if (RealtimeClient.state != RealtimeClient.STATE_ONLINE) {
                dotsCount = (dotsCount + 1) % 4
                titleTv.text = "Подключение" + ".".repeat(dotsCount)
            }
            dotsHandler.postDelayed(this, 450)
        }
    }

    private val stateListener: (String) -> Unit = { s ->
        runOnUiThread {
            when (s) {
                RealtimeClient.STATE_ONLINE -> {
                    titleTv.text = if (Store.testMode) "Nexus · ТЕСТ" else "Nexus"
                    loadChats(true)
                }
                RealtimeClient.STATE_OFFLINE -> titleTv.text = "Nexus · офлайн"
                else -> Unit
            }
        }
    }

    private val msgListener: (RtMessage) -> Unit = { m ->
        runOnUiThread {
            if (m.senderId != Store.user?.id) {
                loadChats(true)
                val title = m.chatTitle ?: m.senderName
                Notify.message(this, title, m.text.ifEmpty { "📎 Вложение" }, m.chatId, m.chatTitle)
            }
        }
    }

    private val avatarColors = intArrayOf(
        0xFFE17076.toInt(), 0xFF7BC862.toInt(), 0xFF65AADD.toInt(),
        0xFFA695E7.toInt(), 0xFFEE7AAE.toInt(), 0xFF6EC9CB.toInt(), 0xFFFAA774.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 77)
        }

        val frame = FrameLayout(this)
        frame.setBackgroundColor(color(R.color.bgPrimary))

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(8), dp(10))
        }
        val logo = TextView(this).apply {
            text = "N"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.textPrimary))
            paint.isFakeBoldText = true
            background = Ui.tile(this@ChatsActivity, color(R.color.accent))
        }
        header.addView(logo, LinearLayout.LayoutParams(dp(36), dp(36)))
        titleTv = Ui.text(this, if (Store.testMode) "Nexus · ТЕСТ" else "Nexus", 22f, R.color.textPrimary, true)
        header.addView(titleTv,
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(12) })
        val dots = ImageView(this).apply {
            setImageResource(R.drawable.ic_dots)
            imageTintList = ColorStateList.valueOf(color(R.color.textSecondary))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        dots.setOnClickListener { showMenu() }
        header.addView(dots, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val search = EditText(this).apply {
            hint = "Поиск чатов"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = Ui.pill(this@ChatsActivity, R.color.bgInput)
            val d = resources.getDrawable(R.drawable.ic_search, null)
            d.setTint(color(R.color.textMuted))
            setCompoundDrawablesWithIntrinsicBounds(d, null, null, null)
            compoundDrawablePadding = dp(12)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            textSize = 15f
            maxLines = 1
        }
        root.addView(search, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            leftMargin = dp(12); rightMargin = dp(12); bottomMargin = dp(6)
        })

        val chipsScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        chipsBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), 0, dp(12), dp(8))
        }
        chipsScroll.addView(chipsBar, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        root.addView(chipsScroll, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        chatAdapter = object : ArrayAdapter<Chat>(this@ChatsActivity, 0, chats) {
            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View {
                val chat = getItem(pos)!!
                val muted = LocalPrefs.isMuted(this@ChatsActivity, chat.id)
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(10), dp(16), dp(10))
                }

                val avatar = TextView(context).apply {
                    text = (chat.title ?: "Ч").take(1).uppercase()
                    textSize = 20f
                    gravity = Gravity.CENTER
                    setTextColor(0xFFFFFFFF.toInt())
                    paint.isFakeBoldText = true
                    background = Ui.tileCircle(context, avatarColors[Math.abs(chat.id.hashCode()) % avatarColors.size])
                }
                row.addView(avatar, LinearLayout.LayoutParams(dp(54), dp(54)))

                val mid = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

                val line1 = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                line1.addView(
                    Ui.text(context, chat.title ?: "Чат", 16f, R.color.textPrimary, true),
                    LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                )
                if (muted) {
                    line1.addView(ImageView(context).apply {
                        setImageResource(R.drawable.ic_bell_off)
                        imageTintList = ColorStateList.valueOf(color(R.color.textMuted))
                    }, LinearLayout.LayoutParams(dp(14), dp(14)).apply { leftMargin = dp(6) })
                }
                line1.addView(Ui.text(context, formatTime(chat.lastMessageAt), 12f,
                    if (chat.unreadCount > 0) R.color.accentText else R.color.textMuted))
                mid.addView(line1)

                val line2 = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                line2.addView(
                    Ui.text(context, chat.lastMessage ?: "Нет сообщений", 14f, R.color.textSecondary),
                    LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
                )
                if (chat.unreadCount > 0) {
                    line2.addView(TextView(context).apply {
                        text = "${chat.unreadCount}"
                        textSize = 12f
                        gravity = Gravity.CENTER
                        setTextColor(0xFFFFFFFF.toInt())
                        paint.isFakeBoldText = true
                        background = Ui.pill(context, R.color.badge)
                        minWidth = dp(22)
                        setPadding(dp(7), dp(2), dp(7), dp(2))
                    }, LinearLayout.LayoutParams(WRAP_CONTENT, dp(20)).apply { leftMargin = dp(8) })
                }
                mid.addView(line2, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(3) })

                row.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(14) })
                return row
            }
        }

        listView = ListView(this).apply {
            adapter = chatAdapter
            setBackgroundColor(color(R.color.bgPrimary))
            divider = null
            dividerHeight = 0
            clipToPadding = false
            setPadding(0, dp(4), 0, dp(110))
        }
        root.addView(listView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        emptyText = TextView(this).apply {
            text = "Нет чатов"
            gravity = Gravity.CENTER
            textSize = 15f
            setTextColor(color(R.color.textMuted))
            visibility = View.GONE
        }
        root.addView(emptyText, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        frame.addView(root, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val fab = ImageView(this).apply {
            setImageResource(R.drawable.ic_pencil)
            imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
            background = Ui.pill(this@ChatsActivity, R.color.accent)
            elevation = dp(6).toFloat()
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        fab.setOnClickListener { createChat() }
        frame.addView(fab, FrameLayout.LayoutParams(dp(56), dp(56)).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            rightMargin = dp(16); bottomMargin = dp(88)
        })

        BottomNav.attach(frame, this, "chats")
        setContentView(frame)

        listView.setOnItemClickListener { _, _, pos, _ ->
            val chat = chatAdapter.getItem(pos) ?: return@setOnItemClickListener
            startActivity(Intent(this, ChatActivity::class.java)
                .putExtra("chatId", chat.id)
                .putExtra("title", chat.title))
        }

        listView.setOnItemLongClickListener { _, _, pos, _ ->
            val chat = chatAdapter.getItem(pos) ?: return@setOnItemLongClickListener true
            val muted = LocalPrefs.isMuted(this, chat.id)
            NxDialog(this).title(chat.title ?: "Чат")
                .items(listOf(
                    if (chat.pinned) "Открепить" else "Закрепить",
                    if (muted) "Включить звук" else "Без звука",
                    "Информация"
                )) { i ->
                    when (i) {
                        0 -> {
                            val path = if (chat.pinned) "/chats/${chat.id}/unpin" else "/chats/${chat.id}/pin"
                            Api.post(path, JSONObject()) { _, _ -> loadChats(true) }
                        }
                        1 -> {
                            LocalPrefs.setMuted(this, chat.id, !muted)
                            runOnUiThread { chatAdapter.notifyDataSetChanged() }
                            Ui.snackbar(this, if (muted) "Звук включён" else "Чат без звука")
                        }
                        2 -> startActivity(Intent(this, GroupInfoActivity::class.java)
                            .putExtra("chatId", chat.id)
                            .putExtra("title", chat.title))
                    }
                }.show()
            true
        }

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { chatAdapter.filter.filter(s) }
        })

        RealtimeClient.retain(this)
        RealtimeClient.addStateListener(stateListener)
        RealtimeClient.addMessageListener(msgListener)
        stateListener(RealtimeClient.state)
        dotsHandler.post(dotsTask)
    }

    override fun onResume() {
        super.onResume()
        buildChips()
        loadChats(false)
    }

    override fun onDestroy() {
        dotsHandler.removeCallbacks(dotsTask)
        RealtimeClient.removeStateListener(stateListener)
        RealtimeClient.removeMessageListener(msgListener)
        RealtimeClient.release()
        super.onDestroy()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun buildChips() {
        chipsBar.removeAllViews()
        addChip("Все", null)
        FoldersStore.all(this).forEach { f -> addChip(f.name, f) }
    }

    private fun addChip(label: String, folder: ChatFolder?) {
        val selected = (folder == null && currentFolder == null) || (folder != null && currentFolder?.id == folder.id)
        val chip = TextView(this).apply {
            text = label
            textSize = 13f
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setTextColor(color(if (selected) R.color.textPrimary else R.color.textSecondary))
            paint.isFakeBoldText = selected
            background = Ui.pill(this@ChatsActivity, if (selected) R.color.accent else R.color.bgSecondary)
        }
        chip.setOnClickListener {
            currentFolder = folder
            buildChips()
            applyFilter()
        }
        chipsBar.addView(chip, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            rightMargin = dp(8)
        })
    }

    private fun applyFilter() {
        chats.clear()
        val src = if (currentFolder == null) allChatsList
        else allChatsList.filter { c -> currentFolder!!.chatIds.contains(c.id) }
        chats.addAll(src.sortedByDescending { it.pinned })
        chatAdapter.notifyDataSetChanged()
        emptyText.text = if (currentFolder != null) "В папке пусто" else "Нет чатов"
        emptyText.visibility = if (chats.isEmpty()) View.VISIBLE else View.GONE
        listView.visibility = if (chats.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showMenu() {
        NxDialog(this).items(listOf("Обновить", "Создать группу", "Адрес сервера")) { i ->
            when (i) {
                0 -> loadChats(true)
                1 -> startActivity(Intent(this, CreateGroupActivity::class.java))
                2 -> showServerDialog()
            }
        }.show()
    }

    private fun showServerDialog() {
        val input = EditText(this).apply {
            setText(Store.apiBase)
            setTextColor(color(R.color.textPrimary))
            setHintTextColor(color(R.color.textMuted))
            background = Ui.pill(this@ChatsActivity, R.color.bgInput)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        NxDialog(this)
            .title("Адрес сервера")
            .view(input)
            .button("Сохранить") {
                Store.apiBase = input.text.toString().trim()
                Ui.snackbar(this, "Сервер: ${Store.apiBase}")
            }
            .button("Отмена") {}
            .show()
    }

    private fun loadChats(force: Boolean) {
        Cache.get(this, "chats")?.let { raw -> renderChats(raw) }
        if (LocalPrefs.powerSave && !force) return
        Api.get("/chats") { code, body ->
            runOnUiThread {
                if (code == 200) {
                    Cache.put(this, "chats", body)
                    renderChats(body)
                } else if (code != 0 && !Store.testMode) {
                    Ui.snackbar(this, Api.friendlyError(body))
                }
            }
        }
    }

    private fun renderChats(body: String) {
        allChatsList.clear()
        val arr = Api.parseArray(body)
        for (i in 0 until arr.length()) allChatsList.add(Chat.fromJson(arr.getJSONObject(i)))
        applyFilter()
    }

    private fun createChat() {
        val input = EditText(this).apply {
            hint = "Название чата"
            setTextColor(color(R.color.textPrimary))
            setHintTextColor(color(R.color.textMuted))
            background = Ui.pill(this@ChatsActivity, R.color.bgInput)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        NxDialog(this)
            .title("Новый чат")
            .view(input)
            .button("Создать") {
                val title = input.text.toString().trim()
                Api.post("/chats", JSONObject().put("title", title).put("userIds", JSONArray())) { code, _ ->
                    runOnUiThread {
                        if (code in 200..299) loadChats(true)
                        else Ui.snackbar(this, "Не удалось создать чат")
                    }
                }
            }
            .button("Отмена") {}
            .show()
    }

    private fun formatTime(s: String?): String {
        if (s.isNullOrEmpty()) return ""
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(s) ?: return ""
            val diff = (System.currentTimeMillis() - date.time) / 86400000L
            when {
                diff == 0L -> SimpleDateFormat("HH:mm", LocalPrefs.formatLocale()).format(date)
                diff == 1L -> "Вчера"
                diff < 7 -> SimpleDateFormat("EEE", LocalPrefs.formatLocale()).format(date)
                else -> SimpleDateFormat("dd.MM", LocalPrefs.formatLocale()).format(date)
            }
        } catch (e: Exception) { "" }
    }
}