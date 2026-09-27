package com.nexus.messenger

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
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

class InviteActivity : Activity() {
    private lateinit var chatId: String
    private lateinit var invitesList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getStringExtra("chatId") ?: run { finish(); return }
        val title = intent.getStringExtra("title") ?: "Группа"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }
        root.addView(header("Ссылки-приглашения"))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        val createCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.card(this@InviteActivity)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        createCard.addView(Ui.tileIcon(this, R.drawable.ic_person_add, 0xFF4D7EC2.toInt()),
            LinearLayout.LayoutParams(dp(40), dp(40)))
        createCard.addView(Ui.text(this, "Создать новую ссылку", 16f, R.color.accentText),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
        createCard.setOnClickListener { createInvite() }
        content.addView(createCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        content.addView(Ui.text(this, "Активные ссылки", 13f, R.color.accentText, true)
            .apply { setPadding(dp(4), dp(16), dp(4), dp(6)) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        invitesList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@InviteActivity)
        }
        content.addView(invitesList, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        content.addView(Ui.text(this, "По ссылке любой может вступить в группу. Отозвать можно в любой момент.", 12f, R.color.textMuted),
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
        val back = TextView(this@InviteActivity).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        addView(back)
        addView(Ui.text(this@InviteActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }

    private fun load() {
        Api.get("/chats/$chatId/invites") { code, body ->
            runOnUiThread {
                invitesList.removeAllViews()
                if (code != 200) {
                    invitesList.addView(Ui.text(this, "Не удалось загрузить", 14f, R.color.textMuted)
                        .apply { setPadding(dp(16), dp(14), dp(16), dp(14)) })
                    return@runOnUiThread
                }
                val arr = Api.parseArray(body)
                if (arr.length() == 0) {
                    invitesList.addView(Ui.text(this, "Пока нет ссылок-приглашений", 14f, R.color.textMuted)
                        .apply { setPadding(dp(16), dp(14), dp(16), dp(14)) })
                    return@runOnUiThread
                }
                for (i in 0 until arr.length()) {
                    val inv = arr.optJSONObject(i) ?: continue
                    val id = inv.optString("id")
                    val code = inv.optString("code")
                    val uses = inv.optInt("uses", 0)
                    val creator = inv.optJSONObject("creator")?.optString("username") ?: "—"
                    val link = "nexus://join/$code"

                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                    }
                    val top = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    top.addView(Ui.text(this, link, 13f, R.color.accentText, true),
                        LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
                    top.addView(Ui.text(this, "исп. $uses", 11f, R.color.textMuted))
                    row.addView(top, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    row.addView(Ui.text(this, "Создал @$creator", 12f, R.color.textSecondary),
                        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })

                    val actions = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.END
                    }
                    val copyBtn = TextView(this).apply {
                        text = "Скопировать"
                        textSize = 13f
                        setTextColor(color(R.color.accentText))
                        setPadding(dp(8), dp(8), dp(8), dp(8))
                    }
                    copyBtn.setOnClickListener {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("nexus", link))
                        Ui.snackbar(this@InviteActivity, "Скопировано ✓")
                    }
                    actions.addView(copyBtn)
                    val revokeBtn = TextView(this).apply {
                        text = "Отозвать"
                        textSize = 13f
                        setTextColor(color(R.color.danger))
                        setPadding(dp(8), dp(8), dp(8), dp(8))
                    }
                    revokeBtn.setOnClickListener {
                        NxDialog(this@InviteActivity)
                            .message("Отозвать ссылку? Больше никто не сможет по ней вступить.")
                            .button("Отозвать") {
                                Api.delete("/chats/$chatId/invites/$id") { c2, _ ->
                                    runOnUiThread {
                                        if (c2 in 200..299) {
                                            Ui.snackbar(this@InviteActivity, "Ссылка отозвана")
                                            load()
                                        } else {
                                            Ui.snackbar(this@InviteActivity, "Не удалось отозвать")
                                        }
                                    }
                                }
                            }
                            .button("Отмена") {}
                            .show()
                    }
                    actions.addView(revokeBtn)
                    row.addView(actions, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(4) })

                    invitesList.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    if (i < arr.length() - 1) {
                        invitesList.addView(View(this).apply { setBackgroundColor(color(R.color.divider)) },
                            LinearLayout.LayoutParams(MATCH_PARENT, 1).apply { leftMargin = dp(16) })
                    }
                }
            }
        }
    }

    private fun createInvite() {
        Api.post("/chats/$chatId/invites", JSONObject()) { code, body ->
            runOnUiThread {
                if (code in 200..299) {
                    Ui.snackbar(this, "Ссылка создана")
                    load()
                } else {
                    Ui.snackbar(this, "Не удалось создать ссылку")
                }
            }
        }
    }
}