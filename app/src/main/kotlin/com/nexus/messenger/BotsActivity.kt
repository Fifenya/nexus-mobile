package com.nexus.messenger

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONObject

class BotsActivity : Activity() {
    private lateinit var listCard: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }

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
        header.addView(Ui.text(this, "Мои боты", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        val createCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.card(this@BotsActivity)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        createCard.addView(Ui.tileIcon(this, R.drawable.ic_bot, 0xFFE09A3F.toInt()),
            LinearLayout.LayoutParams(dp(40), dp(40)))
        createCard.addView(Ui.text(this, "Создать нового бота", 16f, R.color.accentText),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
        createCard.setOnClickListener { showCreateDialog() }
        content.addView(createCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        content.addView(Ui.text(this, "Существующие боты", 13f, R.color.accentText, true)
            .apply { setPadding(dp(4), dp(16), dp(4), dp(6)) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        listCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@BotsActivity)
        }
        content.addView(listCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)

        load()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun load() {
        Api.get("/bots") { code, body ->
            runOnUiThread {
                listCard.removeAllViews()
                if (code != 200) {
                    listCard.addView(Ui.text(this, "Сервер ботов недоступен", 14f, R.color.textMuted)
                        .apply { setPadding(dp(16), dp(14), dp(16), dp(14)) })
                    return@runOnUiThread
                }
                val arr = Api.parseArray(body)
                if (arr.length() == 0) {
                    listCard.addView(Ui.text(this, "У вас пока нет ботов", 14f, R.color.textMuted)
                        .apply { setPadding(dp(16), dp(14), dp(16), dp(14)) })
                    return@runOnUiThread
                }
                for (i in 0 until arr.length()) {
                    val b = arr.optJSONObject(i) ?: continue
                    val name = b.optString("name")
                    val username = b.optString("username")
                    val token = b.optString("token")
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(16), dp(12), dp(16), dp(12))
                    }
                    row.addView(Ui.tileIcon(this, R.drawable.ic_bot, 0xFFE09A3F.toInt()),
                        LinearLayout.LayoutParams(dp(40), dp(40)))
                    val mid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                    mid.addView(Ui.text(this, name, 16f, R.color.textPrimary, true),
                        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    mid.addView(Ui.text(this, "@$username", 13f, R.color.textSecondary),
                        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
                    row.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
                    row.setOnClickListener {
                        NxDialog(this@BotsActivity).title(name)
                            .message("Токен бота:\n$token\n\nИспользуйте этот токен для авторизации в вебхуках и команд.")
                            .button("OK") {}
                            .show()
                    }
                    listCard.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                    if (i < arr.length() - 1) {
                        listCard.addView(View(this).apply { setBackgroundColor(color(R.color.divider)) },
                            LinearLayout.LayoutParams(MATCH_PARENT, 1).apply { leftMargin = dp(72) })
                    }
                }
            }
        }
    }

    private fun showCreateDialog() {
        val nameInput = EditText(this).apply {
            hint = "Имя бота"
            setTextColor(color(R.color.textPrimary))
            setHintTextColor(color(R.color.textMuted))
            background = Ui.pill(this@BotsActivity, R.color.bgInput)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val userInput = EditText(this).apply {
            hint = "Имя пользователя (@username)"
            setTextColor(color(R.color.textPrimary))
            setHintTextColor(color(R.color.textMuted))
            background = Ui.pill(this@BotsActivity, R.color.bgInput)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        NxDialog(this)
            .title("Новый бот")
            .view(nameInput)
            .view(userInput)
            .button("Создать") {
                val body = JSONObject()
                    .put("name", nameInput.text.toString().trim())
                    .put("username", userInput.text.toString().trim())
                Api.post("/bots", body) { code, _ ->
                    runOnUiThread {
                        if (code in 200..299) {
                            Ui.snackbar(this, "Бот создан")
                            load()
                        } else {
                            Ui.snackbar(this, "Не удалось создать бота")
                        }
                    }
                }
            }
            .button("Отмена") {}
            .show()
    }
}