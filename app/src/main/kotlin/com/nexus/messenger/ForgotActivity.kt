package com.nexus.messenger

import android.app.Activity
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
import com.nexus.messenger.ui.Backgrounds
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONObject

class ForgotActivity : Activity() {
    private lateinit var username: EditText
    private lateinit var lastPassword: EditText
    private lateinit var step2Card: LinearLayout
    private lateinit var codeInput: EditText
    private lateinit var newPass: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this)
        Backgrounds.attach(frame)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0x00000000)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(14), dp(16), dp(10))
        }
        val back = TextView(this).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        header.addView(back)
        header.addView(
            Ui.text(this, "Восстановление пароля", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) }
        )
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(32))
        }

        // ── Шаг 1 ──
        val card1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@ForgotActivity)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        card1.addView(Ui.text(this, "Шаг 1. Подтверждение личности", 13f, R.color.accentText, true)
            .apply { setPadding(dp(12), dp(10), dp(12), dp(4)) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        username = input("Имя пользователя", false)
        card1.addView(username, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        card1.addView(divider(), LinearLayout.LayoutParams(MATCH_PARENT, 1).apply {
            leftMargin = dp(16); rightMargin = dp(16)
        })
        lastPassword = input("Последний пароль, который помните", true)
        card1.addView(lastPassword, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        content.addView(card1, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val btnGet = TextView(this).apply {
            text = "Запросить код"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.pill(this@ForgotActivity, R.color.accent)
            elevation = dp(6).toFloat()
        }
        content.addView(btnGet, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)).apply { topMargin = dp(16) })

        content.addView(
            Ui.text(this, "Код придёт администратору в чат «Моты» в Nexus. Он передаст его тебе лично.", 12f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) }
        )

        // ── Шаг 2 ──
        step2Card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@ForgotActivity)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            visibility = android.view.View.GONE
        }
        step2Card.addView(Ui.text(this, "Шаг 2. Код и новый пароль", 13f, R.color.accentText, true)
            .apply { setPadding(dp(12), dp(10), dp(12), dp(4)) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        codeInput = input("Код из 6 цифр", false)
        codeInput.inputType = 0x00000002
        step2Card.addView(codeInput, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        step2Card.addView(divider(), LinearLayout.LayoutParams(MATCH_PARENT, 1).apply {
            leftMargin = dp(16); rightMargin = dp(16)
        })
        newPass = input("Новый пароль (мин. 6 символов)", true)
        step2Card.addView(newPass, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        content.addView(step2Card, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(20)
        })

        val btnReset = TextView(this).apply {
            text = "Сменить пароль"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.pill(this@ForgotActivity, R.color.accent)
            elevation = dp(6).toFloat()
            visibility = android.view.View.GONE
        }
        content.addView(btnReset, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)).apply { topMargin = dp(16) })

        scroll.addView(content)
        frame.addView(scroll, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(frame)

        btnGet.setOnClickListener {
            val u = username.text.toString().trim()
            if (u.isEmpty()) {
                Ui.snackbar(this, "Укажите имя пользователя")
                return@setOnClickListener
            }
            btnGet.text = "Отправка..."
            Api.post("/auth/forgot", JSONObject()
                .put("username", u)
                .put("lastPassword", lastPassword.text.toString())
            ) { code, body ->
                runOnUiThread {
                    btnGet.text = "Запросить код"
                    val j = Api.parseObj(body)
                    if (code in 200..299) {
                        step2Card.visibility = android.view.View.VISIBLE
                        btnReset.visibility = android.view.View.VISIBLE
                        Ui.snackbar(this, j?.optString("message") ?: "Код отправлен администратору")
                    } else {
                        NxDialog(this).title("Ошибка")
                            .message(j?.optString("message") ?: Api.friendlyError(body))
                            .button("OK") {}.show()
                    }
                }
            }
        }

        btnReset.setOnClickListener {
            val u = username.text.toString().trim()
            val c = codeInput.text.toString().trim()
            val p = newPass.text.toString()
            if (c.isEmpty() || p.isEmpty()) {
                Ui.snackbar(this, "Заполните код и новый пароль")
                return@setOnClickListener
            }
            btnReset.text = "Проверка..."
            Api.post("/auth/reset", JSONObject()
                .put("username", u)
                .put("code", c)
                .put("newPassword", p)
            ) { code, body ->
                runOnUiThread {
                    btnReset.text = "Сменить пароль"
                    val j = Api.parseObj(body)
                    if (code in 200..299) {
                        Ui.snackbar(this, "Пароль изменён ✓")
                        finish()
                    } else {
                        NxDialog(this).title("Ошибка")
                            .message(j?.optString("message") ?: Api.friendlyError(body))
                            .button("OK") {}.show()
                    }
                }
            }
        }
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun divider() = android.view.View(this).apply {
        setBackgroundColor(color(R.color.divider))
    }

    private fun input(hint: String, isPassword: Boolean): EditText = EditText(this).apply {
        this.hint = hint
        setHintTextColor(color(R.color.textMuted))
        setTextColor(color(R.color.textPrimary))
        background = null
        setPadding(dp(16), dp(16), dp(16), dp(16))
        textSize = 16f
        maxLines = 1
        if (isPassword) inputType = 0x00000081
    }
}