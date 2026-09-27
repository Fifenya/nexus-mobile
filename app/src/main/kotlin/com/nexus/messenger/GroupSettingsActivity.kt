package com.nexus.messenger

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONObject

class GroupSettingsActivity : Activity() {
    private lateinit var chatId: String
    private lateinit var reactionsValue: TextView
    private lateinit var slowModeValue: TextView
    private lateinit var sendValue: TextView
    private lateinit var inviteValue: TextView
    private lateinit var pinValue: TextView

    private var reactions = "all"
    private var slowMode = 0
    private var sendMessages = "all"
    private var inviteUsers = "all"
    private var pinMessages = "admin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getStringExtra("chatId") ?: run { finish(); return }
        val title = intent.getStringExtra("title") ?: "Группа"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }
        root.addView(header("Настройки группы"))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        // ── Реакции и slow mode ──
        val chatCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@GroupSettingsActivity)
        }
        chatCard.addView(sectionLabel("Чат"))

        reactionsValue = Ui.text(this, "", 14f, R.color.accentText)
        chatCard.addView(tapRow("Реакции на сообщения", reactionsValue) {
            NxDialog(this).title("Реакции")
                .items(listOf("Все пользователи", "Отключены")) { i ->
                    reactions = if (i == 0) "all" else "disabled"
                    save()
                }.show()
        })
        chatCard.addView(dividerInset())

        slowModeValue = Ui.text(this, "", 14f, R.color.accentText)
        chatCard.addView(tapRow("Медленный режим", slowModeValue) {
            NxDialog(this).title("Задержка между сообщениями")
                .items(listOf("Выкл.", "10 секунд", "30 секунд", "1 минута", "5 минут")) { i ->
                    slowMode = when (i) { 0 -> 0; 1 -> 10; 2 -> 30; 3 -> 60; 4 -> 300; else -> 0 }
                    save()
                }.show()
        })
        content.addView(chatCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // ── Разрешения ──
        val permCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@GroupSettingsActivity)
        }
        permCard.addView(sectionLabel("Разрешения участников"))

        sendValue = Ui.text(this, "", 14f, R.color.accentText)
        permCard.addView(tapRow("Отправка сообщений", sendValue) {
            NxDialog(this).title("Кто может отправлять сообщения")
                .items(listOf("Все участники", "Только администраторы")) { i ->
                    sendMessages = if (i == 0) "all" else "admin"
                    save()
                }.show()
        })
        permCard.addView(dividerInset())

        inviteValue = Ui.text(this, "", 14f, R.color.accentText)
        permCard.addView(tapRow("Добавление участников", inviteValue) {
            NxDialog(this).title("Кто может добавлять участников")
                .items(listOf("Все участники", "Только администраторы")) { i ->
                    inviteUsers = if (i == 0) "all" else "admin"
                    save()
                }.show()
        })
        permCard.addView(dividerInset())

        pinValue = Ui.text(this, "", 14f, R.color.accentText)
        permCard.addView(tapRow("Закрепление сообщений", pinValue) {
            NxDialog(this).title("Кто может закреплять сообщения")
                .items(listOf("Все участники", "Только администраторы")) { i ->
                    pinMessages = if (i == 0) "all" else "admin"
                    save()
                }.show()
        })
        content.addView(permCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })

        content.addView(Ui.text(this, "Изменения применяются ко всем участникам группы.", 12f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)

        load()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun header(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(color(R.color.bgSecondary))
        setPadding(dp(4), dp(10), dp(16), dp(10))
        val back = TextView(this@GroupSettingsActivity).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        addView(back)
        addView(Ui.text(this@GroupSettingsActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }

    private fun sectionLabel(s: String): TextView =
        Ui.text(this, s, 13f, R.color.accentText, true).apply { setPadding(dp(16), dp(14), dp(16), dp(6)) }

    private fun dividerInset() = android.view.View(this).apply { setBackgroundColor(color(R.color.divider)) }

    private fun tapRow(title: String, valueView: TextView, onClick: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(Ui.text(this@GroupSettingsActivity, title, 16f, R.color.textPrimary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(valueView)
            setOnClickListener { onClick() }
        }

    private fun load() {
        Api.get("/chats/$chatId/settings") { code, body ->
            runOnUiThread {
                if (code != 200) return@runOnUiThread
                val j = Api.parseObj(body) ?: return@runOnUiThread
                reactions = j.optString("reactions", "all")
                slowMode = j.optInt("slowMode", 0)
                val perm = j.optJSONObject("permissions") ?: JSONObject()
                sendMessages = perm.optString("sendMessages", "all")
                inviteUsers = perm.optString("inviteUsers", "all")
                pinMessages = perm.optString("pinMessages", "admin")
                renderValues()
            }
        }
    }

    private fun renderValues() {
        reactionsValue.text = if (reactions == "all") "Все пользователи" else "Отключены"
        slowModeValue.text = when (slowMode) {
            0 -> "Выкл."
            10 -> "10 секунд"
            30 -> "30 секунд"
            60 -> "1 минута"
            300 -> "5 минут"
            else -> "—"
        }
        sendValue.text = if (sendMessages == "all") "Все участники" else "Только админы"
        inviteValue.text = if (inviteUsers == "all") "Все участники" else "Только админы"
        pinValue.text = if (pinMessages == "all") "Все участники" else "Только админы"
    }

    private fun save() {
        val body = JSONObject().apply {
            put("reactions", reactions)
            put("slowMode", slowMode)
            put("permissions", JSONObject().apply {
                put("sendMessages", sendMessages)
                put("inviteUsers", inviteUsers)
                put("pinMessages", pinMessages)
            })
        }
        Api.patch("/chats/$chatId/settings", body) { code, _ ->
            runOnUiThread {
                renderValues()
                Ui.snackbar(this, if (code in 200..299) "Сохранено" else "Не удалось сохранить")
            }
        }
    }
}