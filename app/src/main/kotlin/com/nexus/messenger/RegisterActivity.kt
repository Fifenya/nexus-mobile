package com.nexus.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.User
import com.nexus.messenger.ui.Backgrounds
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONObject

class RegisterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this)
        Backgrounds.attach(frame)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(80), dp(28), dp(28))
        }

        root.addView(Ui.text(this, "Создать аккаунт", 28f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        root.addView(Ui.text(this, "Присоединяйтесь к Nexus", 14f, R.color.textSecondary),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(6) })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@RegisterActivity)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val fields = mutableListOf<EditText>()
        val hints = listOf("Имя пользователя", "Пароль", "Повторите пароль")
        val types = listOf(1, 0x81, 0x81)
        hints.forEachIndexed { i, hint ->
            val e = EditText(this).apply {
                this.hint = hint
                inputType = types[i]
                setHintTextColor(color(R.color.textMuted))
                setTextColor(color(R.color.textPrimary))
                background = null
                setPadding(dp(16), dp(16), dp(16), dp(16))
                textSize = 16f
                maxLines = 1
            }
            card.addView(e, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            if (i < hints.size - 1) {
                card.addView(android.view.View(this).apply {
                    setBackgroundColor(color(R.color.divider))
                }, LinearLayout.LayoutParams(MATCH_PARENT, 1).apply {
                    leftMargin = dp(16); rightMargin = dp(16)
                })
            }
            fields.add(e)
        }
        root.addView(card, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(32) })

        val btn = TextView(this).apply {
            text = "Создать аккаунт"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.pill(this@RegisterActivity, R.color.accent)
            elevation = dp(6).toFloat()
        }
        root.addView(btn, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)).apply { topMargin = dp(18) })

        val link = TextView(this).apply {
            text = "Уже есть аккаунт? Войти"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.accentText))
            setPadding(dp(16), dp(18), dp(16), dp(8))
        }
        link.setOnClickListener { finish() }
        root.addView(link, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))

        scroll.addView(root, ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        frame.addView(scroll, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(frame)

        btn.setOnClickListener {
            val u = fields[0].text.toString().trim()
            val p = fields[1].text.toString()
            val p2 = fields[2].text.toString()
            if (u.isEmpty() || p.isEmpty()) {
                Ui.snackbar(this, "Заполните обязательные поля")
                return@setOnClickListener
            }
            if (p != p2) {
                Ui.snackbar(this, "Пароли не совпадают")
                return@setOnClickListener
            }
            btn.text = "Создание..."
            Api.post("/auth/register", JSONObject().put("username", u).put("password", p)) { code, body ->
                runOnUiThread {
                    btn.text = "Создать аккаунт"
                    val j = Api.parseObj(body)
                    if ((code == 200 || code == 201) && j != null) {
                        Store.testMode = false
                        Store.token = j.optString("accessToken").takeIf { it.isNotEmpty() }
                            ?: j.optString("access_token").takeIf { it.isNotEmpty() }
                            ?: j.optString("token").takeIf { it.isNotEmpty() }
                        j.optJSONObject("user")?.let { Store.user = User.fromJson(it) }
                        startActivity(Intent(this, ChatsActivity::class.java))
                        finish()
                    } else {
                        NxDialog(this)
                            .title("Ошибка")
                            .message(Api.friendlyError(body))
                            .button("OK") {}
                            .show()
                    }
                }
            }
        }
    }

    private fun color(res: Int): Int = resources.getColor(res, null)
}