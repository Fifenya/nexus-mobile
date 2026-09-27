package com.nexus.messenger

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.FrameLayout
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
    private lateinit var reactionsSub: TextView
    private lateinit var slowSub: TextView
    private lateinit var sendSub: TextView
    private lateinit var inviteSub: TextView
    private lateinit var pinSub: TextView

    private var reactions = "all"
    private var slowMode = 0
    private var sendPerm = "all"
    private var invitePerm = "all"
    private var pinPerm = "admin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getStringExtra("chatId") ?: run { finish(); return }

        val frame = FrameLayout(this)
        frame.setBackgroundColor(color(R.color.bgPrimary))

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(color(R.color.bgSecondary))
            setPadding(dp(4), dp(10), dp(16), dp(10))
        }
        val back = TextView(this).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        header.addView(back)
        header.addView(Ui.text(this, "Настройки группы", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@GroupSettingsActivity)
        }
        card.addView(sectionLabel("Общие"))

        reactionsSub = Ui.text(this, reactionsLabel(), 14f, R.color.textSecondary)
        card.addView(row("Реакции", reactionsSub) {
            NxDialog(this).title("Кто может ставить реакции")
                .items(listOf("Все участники", "Только админы", "Выключены")) { i ->
                    reactions = when (i) { 0 -> "all"; 1 -> "admin"; else -> "off" }
                    reactionsSub.text = reactionsLabel()
                }.show()
        })
        card.addView(divider())

        slowSub = Ui.text(this, slowLabel(), 14f, R.color.textSecondary)
        card.addView(row("Медленный режим", slowSub) {
            NxDialog(this).title("Интервал сообщений")
                .items(listOf("Выключен", "10 секунд", "30 секунд", "1 минута", "5 минут")) { i ->
                    slowMode = when (i) { 0 -> 0; 1 -> 10; 2 -> 30; 3 -> 60; else -> 300 }
                    slowSub.text = slowLabel()
                }.show()
        })

        card.addView(sectionLabel("Разрешения"))

        sendSub = Ui.text(this, permLabel(sendPerm), 14f, R.color.textSecondary)
        card.addView(row("Отправка сообщений", sendSub) {
            NxDialog(this).title("Кто может писать")
                .items(listOf("Все участники", "Только админы")) { i ->
                    sendPerm = if (i == 0) "all" else "admin"
                    sendSub.text = permLabel(sendPerm)
                }.show()
        })
        card.addView(divider())

        inviteSub = Ui.text(this, permLabel(invitePerm), 14f, R.color.textSecondary)
        card.addView(row("Приглашение участников", inviteSub) {
            NxDialog(this).title("Кто может приглашать")
                .items(listOf("Все участники", "Только админы")) { i ->
                    invitePerm = if (i == 0) "all" else "admin"
                    inviteSub.text = permLabel(invitePerm)
                }.show()
        })
        card.addView(divider())

        pinSub = Ui.text(this, permLabel(pinPerm), 14f, R.color.textSecondary)
        card.addView(row("Закрепление сообщений", pinSub) {
            NxDialog(this).title("Кто может закреплять")
                .items(listOf("Все участники", "Только админы")) { i ->
                    pinPerm = if (i == 0) "all" else "admin"
                    pinSub.text = permLabel(pinPerm)
                }.show()
        })

        content.addView(card, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val saveBtn = TextView(this).apply {
            text = "Сохранить"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.pill(this@GroupSettingsActivity, R.color.accent)
            elevation = dp(6).toFloat()
        }
        saveBtn.setOnClickListener { save() }
        content.addView(saveBtn, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)).apply { topMargin = dp(16) })

        scroll.addView(content, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        frame.addView(scroll, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        frame.addView(root, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(frame)

        load()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun sectionLabel(s: String): TextView =
        Ui.text(this, s, 13f, R.color.accentText, true).apply { setPadding(dp(16), dp(14), dp(16), dp(6)) }

    private fun divider(): View = View(this).apply { setBackgroundColor(color(R.color.divider)) }

    private fun row(titleText: String, sub: TextView, onClick: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(Ui.text(this@GroupSettingsActivity, titleText, 16f, R.color.textPrimary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(sub)
            setOnClickListener { onClick() }
        }

    private fun reactionsLabel(): String = when (reactions) {
        "all" -> "Все участники"
        "admin" -> "Только админы"
        else -> "Выключены"
    }

    private fun slowLabel(): String = when (slowMode) {
        0 -> "Выключен"
        10 -> "10 секунд"
        30 -> "30 секунд"
        60 -> "1 минута"
        else -> "5 минут"
    }

    private fun permLabel(p: String): String = if (p == "all") "Все участники" else "Только админы"

    private fun load() {
        Api.get("/chats/$chatId/settings") { code, body ->
            runOnUiThread {
                if (code != 200) return@runOnUiThread
                val j = Api.parseObj(body) ?: return@runOnUiThread
                reactions = j.optString("reactions").ifEmpty { "all" }
                slowMode = j.optInt("slowMode", 0)
                val perms = j.optJSONObject("permissions")
                if (perms != null) {
                    sendPerm = perms.optString("sendMessages").ifEmpty { "all" }
                    invitePerm = perms.optString("inviteUsers").ifEmpty { "all" }
                    pinPerm = perms.optString("pinMessages").ifEmpty { "admin" }
                }
                reactionsSub.text = reactionsLabel()
                slowSub.text = slowLabel()
                sendSub.text = permLabel(sendPerm)
                inviteSub.text = permLabel(invitePerm)
                pinSub.text = permLabel(pinPerm)
            }
        }
    }

    private fun save() {
        val body = JSONObject().apply {
            put("reactions", reactions)
            put("slowMode", slowMode)
            put("permissions", JSONObject().apply {
                put("sendMessages", sendPerm)
                put("inviteUsers", invitePerm)
                put("pinMessages", pinPerm)
            })
        }
        Api.patch("/chats/$chatId/settings", body) { code, resp ->
            runOnUiThread {
                if (code in 200..299) {
                    Ui.snackbar(this, "Сохранено ✓")
                    finish()
                } else {
                    Ui.snackbar(this, Api.friendlyError(resp))
                }
            }
        }
    }
}