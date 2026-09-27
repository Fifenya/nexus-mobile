package com.nexus.messenger

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Store
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONArray
import org.json.JSONObject

class GroupInfoActivity : Activity() {
    private lateinit var chatId: String
    private lateinit var titleTv: TextView
    private lateinit var descTv: TextView
    private lateinit var subTv: TextView
    private lateinit var membersCard: LinearLayout
    private lateinit var actionsRow: LinearLayout
    private lateinit var settingsRow: LinearLayout
    private var isOwnerOrAdmin = false

    private val avatarColors = intArrayOf(
        0xFFE17076.toInt(), 0xFF7BC862.toInt(), 0xFF65AADD.toInt(),
        0xFFA695E7.toInt(), 0xFFEE7AAE.toInt(), 0xFF6EC9CB.toInt(), 0xFFFAA774.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getStringExtra("chatId") ?: run { finish(); return }
        val title = intent.getStringExtra("title") ?: "Группа"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(color(R.color.bgSecondary))
            setPadding(dp(4), dp(10), dp(8), dp(10))
        }
        val back = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            imageTintList = ColorStateList.valueOf(color(R.color.accentText))
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        back.setOnClickListener { finish() }
        header.addView(back, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        header.addView(Ui.text(this, "Информация", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
        val dots = ImageView(this).apply {
            setImageResource(R.drawable.ic_dots)
            imageTintList = ColorStateList.valueOf(color(R.color.textSecondary))
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        dots.setOnClickListener { showMenu(title) }
        header.addView(dots, LinearLayout.LayoutParams(dp(44), dp(44)))
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(20), dp(12), dp(32))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val avatar = TextView(this).apply {
            text = title.take(1).uppercase()
            textSize = 36f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.tileCircle(this@GroupInfoActivity, avatarColors[Math.abs(title.hashCode()) % avatarColors.size])
        }
        content.addView(avatar, LinearLayout.LayoutParams(dp(96), dp(96)))
        titleTv = Ui.text(this, title, 22f, R.color.textPrimary, true)
        content.addView(titleTv, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(14) })
        subTv = Ui.text(this, "…", 14f, R.color.textSecondary)
        content.addView(subTv, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(2) })
        descTv = Ui.text(this, "", 14f, R.color.textSecondary)
        descTv.visibility = View.GONE
        content.addView(descTv, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(8)
            leftMargin = dp(20); rightMargin = dp(20)
        })

        actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(20), dp(4), 0)
        }
        actionsRow.addView(actionCard(R.drawable.ic_chat, "Чат") {
            startActivity(Intent(this, ChatActivity::class.java)
                .putExtra("chatId", chatId).putExtra("title", titleTv.text.toString()))
        })
        actionsRow.addView(actionCard(R.drawable.ic_bell, "Звук") {
            Ui.snackbar(this, "Звук чата появится позже")
        })
        actionsRow.addView(actionCard(R.drawable.ic_device, "Видеочат") {
            Ui.snackbar(this, "Видеочаты появятся позже")
        })
        actionsRow.addView(actionCard(R.drawable.ic_logout, "Покинуть") { leave() })
        content.addView(actionsRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // Кнопка «Настройки» (появится после проверки прав)
        settingsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(10), dp(4), 0)
            visibility = View.GONE
        }
        settingsRow.addView(actionCard(R.drawable.ic_gear, "Настройки") {
            startActivity(Intent(this, GroupSettingsActivity::class.java)
                .putExtra("chatId", chatId)
                .putExtra("title", titleTv.text.toString()))
        })
        settingsRow.addView(actionCard(R.drawable.ic_share, "Ссылки") {
            startActivity(Intent(this, InviteActivity::class.java)
                .putExtra("chatId", chatId)
                .putExtra("title", titleTv.text.toString()))
        })
        content.addView(settingsRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        membersCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@GroupInfoActivity)
            gravity = Gravity.START
        }
        content.addView(membersCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(20) })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)

        load()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun actionCard(iconRes: Int, label: String, onClick: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = Ui.card(this@GroupInfoActivity)
            setPadding(dp(6), dp(12), dp(6), dp(10))
            addView(Ui.icon(this@GroupInfoActivity, iconRes, color(R.color.textPrimary)),
                LinearLayout.LayoutParams(dp(22), dp(22)))
            addView(Ui.text(this@GroupInfoActivity, label, 11f, R.color.textPrimary),
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(6) })
            setOnClickListener { onClick() }
        }.also { v ->
            v.layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
        }

    private fun load() {
        Api.get("/chats/$chatId/profile") { code, body ->
            runOnUiThread {
                if (code == 200) {
                    val j = Api.parseObj(body) ?: return@runOnUiThread
                    val chatObj = j.optJSONObject("chat") ?: j
                    val title = chatObj.optString("title").takeIf { it.isNotEmpty() && it != "null" }
                        ?: titleTv.text.toString()
                    titleTv.text = title
                    val desc = chatObj.optString("description").takeIf { it.isNotEmpty() && it != "null" }
                    if (desc != null) {
                        descTv.text = desc
                        descTv.visibility = View.VISIBLE
                    }
                }
            }
        }
        loadMembers()
    }

    private fun loadMembers() {
        Api.get("/chats/$chatId/members") { code, body ->
            runOnUiThread {
                if (code != 200) {
                    subTv.text = "участники недоступны"
                    return@runOnUiThread
                }
                val arr = Api.parseArray(body)
                val count = arr.length()
                subTv.text = "$count участник(ов)"

                val myId = Store.user?.id
                isOwnerOrAdmin = false

                membersCard.removeAllViews()
                membersCard.addView(sectionLabel("Участники"))

                for (i in 0 until count) {
                    val m = arr.optJSONObject(i) ?: continue
                    val uObj = m.optJSONObject("user") ?: JSONObject()
                    val uid = uObj.optString("id")
                    val uname = uObj.optString("username").takeIf { it.isNotEmpty() && it != "null" } ?: "—"
                    val role = m.optString("role", "MEMBER").uppercase()
                    if (uid == myId) {
                        isOwnerOrAdmin = role == "OWNER" || role == "ADMIN"
                    }

                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(16), dp(10), dp(16), dp(10))
                    }
                    row.addView(TextView(this).apply {
                        text = uname.take(1).uppercase()
                        textSize = 16f
                        gravity = Gravity.CENTER
                        setTextColor(0xFFFFFFFF.toInt())
                        paint.isFakeBoldText = true
                        background = Ui.tileCircle(this@GroupInfoActivity, avatarColors[Math.abs(uid.hashCode()) % avatarColors.size])
                    }, LinearLayout.LayoutParams(dp(40), dp(40)))
                    val mid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                    mid.addView(Ui.text(this, uname, 16f, R.color.textPrimary),
                        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    mid.addView(Ui.text(this, if (role == "OWNER") "Владелец" else if (role == "ADMIN") "Админ" else "Участник", 12f, R.color.textSecondary),
                        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
                    row.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(14) })

                    if (role == "OWNER") {
                        row.addView(TextView(this).apply {
                            text = "Владелец"
                            textSize = 11f
                            setTextColor(0xFFC084FC.toInt())
                            background = Ui.pillColor(this@GroupInfoActivity, 0x33C084FC)
                            setPadding(dp(10), dp(4), dp(10), dp(4))
                        })
                    }

                    val canManage = isOwnerOrAdmin && uid != myId
                    row.setOnClickListener {
                        if (canManage) {
                            NxDialog(this).items(listOf("Исключить из группы")) { i ->
                                if (i == 0) kickMember(uid, uname)
                            }.show()
                        }
                    }
                    membersCard.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    if (i < count - 1) {
                        membersCard.addView(View(this).apply {
                            setBackgroundColor(color(R.color.divider))
                        }, LinearLayout.LayoutParams(MATCH_PARENT, 1).apply { leftMargin = dp(70) })
                    }
                }

                if (isOwnerOrAdmin) {
                    val addRow = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                    }
                    addRow.addView(Ui.tileIcon(this, R.drawable.ic_person_add, 0xFF4D7EC2.toInt()),
                        LinearLayout.LayoutParams(dp(40), dp(40)))
                    addRow.addView(Ui.text(this, "Добавить участника", 16f, R.color.accentText),
                        LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(14) })
                    addRow.setOnClickListener { searchUsersToAdd() }
                    membersCard.addView(addRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                        topMargin = dp(8)
                    })
                    settingsRow.visibility = View.VISIBLE
                } else {
                    settingsRow.visibility = View.GONE
                }
            }
        }
    }

    private fun kickMember(uid: String, uname: String) {
        NxDialog(this)
            .message("Исключить $uname из группы?")
            .button("Исключить") {
                Api.delete("/chats/$chatId/members/$uid") { code, _ ->
                    runOnUiThread {
                        if (code in 200..299) {
                            Ui.snackbar(this, "Исключён")
                            loadMembers()
                        } else {
                            Ui.snackbar(this, "Не удалось исключить")
                        }
                    }
                }
            }
            .button("Отмена") {}
            .show()
    }

    private fun searchUsersToAdd() {
        val input = EditText(this).apply {
            hint = "Имя пользователя"
            setTextColor(color(R.color.textPrimary))
            background = Ui.pill(this@GroupInfoActivity, R.color.bgInput)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        NxDialog(this)
            .title("Добавить участника")
            .message("Введите имя пользователя (минимум 2 символа)")
            .view(input)
            .button("Найти") {
                val q = input.text.toString().trim()
                if (q.length < 2) return@button
                Api.get("/users/search?q=${java.net.URLEncoder.encode(q, "UTF-8")}") { code, body ->
                    runOnUiThread {
                        if (code != 200) {
                            Ui.snackbar(this, "Не удалось найти")
                            return@runOnUiThread
                        }
                        val arr = Api.parseArray(body)
                        val names = mutableListOf<String>()
                        val ids = mutableListOf<String>()
                        for (i in 0 until arr.length()) {
                            val u = arr.optJSONObject(i) ?: continue
                            val id = u.optString("id")
                            val uname = u.optString("username")
                            if (id.isNotEmpty()) {
                                names.add("@$uname")
                                ids.add(id)
                            }
                        }
                        if (names.isEmpty()) {
                            Ui.snackbar(this, "Никого не нашли")
                            return@runOnUiThread
                        }
                        NxDialog(this).title("Результаты").items(names) { pick ->
                            val uid = ids[pick]
                            Api.post("/chats/$chatId/members", JSONObject().put("userIds", JSONArray().put(uid))) { code2, _ ->
                                runOnUiThread {
                                    if (code2 in 200..299) {
                                        Ui.snackbar(this, "Добавлен ✓")
                                        loadMembers()
                                    } else {
                                        Ui.snackbar(this, "Не удалось добавить")
                                    }
                                }
                            }
                        }.show()
                    }
                }
            }
            .button("Отмена") {}
            .show()
    }

    private fun sectionLabel(s: String): TextView =
        Ui.text(this, s, 13f, R.color.accentText, true).apply { setPadding(dp(16), dp(14), dp(16), dp(6)) }

    private fun showMenu(currentTitle: String) {
        val items = mutableListOf("Переименовать", "Редактировать описание")
        if (isOwnerOrAdmin) items.add("Настройки группы")
        items.add("Покинуть группу")
        NxDialog(this).items(items) { i ->
            when (items[i]) {
                "Переименовать" -> rename(currentTitle)
                "Редактировать описание" -> editDescription()
                "Настройки группы" -> startActivity(Intent(this, GroupSettingsActivity::class.java)
                    .putExtra("chatId", chatId)
                    .putExtra("title", currentTitle))
                "Покинуть группу" -> leave()
            }
        }.show()
    }

    private fun rename(current: String) {
        val input = EditText(this).apply {
            setText(current)
            setTextColor(color(R.color.textPrimary))
            background = Ui.pill(this@GroupInfoActivity, R.color.bgInput)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        NxDialog(this)
            .title("Новое название")
            .view(input)
            .button("Сохранить") {
                Api.patch("/chats/$chatId", JSONObject().put("title", input.text.toString().trim())) { code, _ ->
                    runOnUiThread {
                        if (code in 200..299) {
                            titleTv.text = input.text.toString().trim()
                            Ui.snackbar(this, "Сохранено")
                        } else {
                            Ui.snackbar(this, "Не удалось переименовать")
                        }
                    }
                }
            }
            .button("Отмена") {}
            .show()
    }

    private fun editDescription() {
        val input = EditText(this).apply {
            hint = "Описание группы"
            setText(descTv.text.toString())
            setTextColor(color(R.color.textPrimary))
            background = Ui.pill(this@GroupInfoActivity, R.color.bgInput)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        NxDialog(this)
            .title("Описание")
            .view(input)
            .button("Сохранить") {
                Api.patch("/chats/$chatId", JSONObject().put("description", input.text.toString().trim())) { code, _ ->
                    runOnUiThread {
                        if (code in 200..299) {
                            descTv.text = input.text.toString().trim()
                            descTv.visibility = if (descTv.text.isEmpty()) View.GONE else View.VISIBLE
                            Ui.snackbar(this, "Описание сохранено")
                        } else {
                            Ui.snackbar(this, "Не удалось сохранить")
                        }
                    }
                }
            }
            .button("Отмена") {}
            .show()
    }

    private fun leave() {
        NxDialog(this)
            .message("Покинуть группу? Вы сможете вернуться по ссылке-приглашению.")
            .button("Покинуть") {
                Api.post("/chats/$chatId/leave", JSONObject()) { code, _ ->
                    runOnUiThread {
                        if (code in 200..299) {
                            Ui.snackbar(this, "Вы покинули группу")
                            finish()
                        } else {
                            Ui.snackbar(this, "Не удалось покинуть")
                        }
                    }
                }
            }
            .button("Отмена") {}
            .show()
    }

    override fun onResume() {
        super.onResume()
        load()
    }
}