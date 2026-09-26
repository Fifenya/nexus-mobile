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

class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this)
        Backgrounds.attach(frame)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(90), dp(28), dp(28))
        }

        val logo = TextView(this).apply {
            text = "N"
            textSize = 46f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            setBackgroundResource(R.drawable.bg_logo)
            elevation = dp(14).toFloat()
        }
        root.addView(logo, LinearLayout.LayoutParams(dp(104), dp(104)))

        root.addView(Ui.text(this, "Nexus", 34f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(22) })
        root.addView(Ui.text(this, "Защищённый мессенджер · v1.0.0", 13f, R.color.textMuted),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(4) })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@LoginActivity)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val username = EditText(this).apply {
            hint = "Имя пользователя"
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = null
            setPadding(dp(16), dp(16), dp(16), dp(16))
            textSize = 16f
            maxLines = 1
        }
        card.addView(username, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        card.addView(divider(), LinearLayout.LayoutParams(MATCH_PARENT, 1).apply {
            leftMargin = dp(16); rightMargin = dp(16)
        })
        val password = EditText(this).apply {
            hint = "Пароль"
            inputType = 0x00000081
            setHintTextColor(color(R.color.textMuted))
            setTextColor(color(R.color.textPrimary))
            background = null
            setPadding(dp(16), dp(16), dp(16), dp(16))
            textSize = 16f
            maxLines = 1
        }
        card.addView(password, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(card, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(36)
        })

        val btn = TextView(this).apply {
            text = "Войти"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.pill(this@LoginActivity, R.color.accent)
            elevation = dp(6).toFloat()
        }
        root.addView(btn, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)).apply { topMargin = dp(18) })

        val reg = TextView(this).apply {
            text = "Нет аккаунта? Создать"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.accentText))
            setPadding(dp(16), dp(18), dp(16), dp(8))
        }
        reg.setOnClickListener { startActivity(Intent(this@LoginActivity, RegisterActivity::class.java)) }
        root.addView(reg, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))

        val serverLink = TextView(this).apply {
            text = "🌐 " + Store.apiBase
            textSize = 11f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(color(R.color.textMuted))
            setPadding(dp(16), dp(8), dp(16), dp(4))
        }
        serverLink.setOnClickListener { showServerDialog() }
        root.addView(serverLink, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(10)
        })

        root.addView(Ui.text(this, "dev-доступ: testdevapp / testdevapp — тест-режим без сервера", 11f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(14) })

        scroll.addView(root, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        frame.addView(scroll, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(frame)

        btn.setOnClickListener {
            val u = username.text.toString().trim()
            val p = password.text.toString()
            if (u.isEmpty() || p.isEmpty()) {
                Ui.snackbar(this, "Заполните все поля")
                return@setOnClickListener
            }

            if (u == "testdevapp" && p == "testdevapp") {
                Store.testMode = true
                Store.token = "mock-token"
                Store.user = User("test-user", "testdevapp", "Test Dev", bio = "Локальный тестовый профиль")
                startActivity(Intent(this, ChatsActivity::class.java))
                finish()
                return@setOnClickListener
            }

            Store.testMode = false
            btn.text = "Вход..."
            Api.post("/auth/login", JSONObject().put("username", u).put("password", p)) { code, body ->
                runOnUiThread {
                    btn.text = "Войти"
                    if (code == 200 || code == 201) {
                        val j = Api.parseObj(body)
                        if (j != null) {
                            Store.token = j.optString("accessToken").takeIf { it.isNotEmpty() }
                                ?: j.optString("access_token").takeIf { it.isNotEmpty() }
                                ?: j.optString("token").takeIf { it.isNotEmpty() }
                            j.optJSONObject("user")?.let { Store.user = User.fromJson(it) }
                            startActivity(Intent(this, ChatsActivity::class.java))
                            finish()
                            return@runOnUiThread
                        }
                    }
                    NxDialog(this)
                        .title("Ошибка входа")
                        .message(Api.friendlyError(body))
                        .button("OK") {}
                        .show()
                }
            }
        }
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun divider() = android.view.View(this).apply {
        setBackgroundColor(color(R.color.divider))
    }

    private fun showServerDialog() {
        val input = EditText(this).apply {
            setText(Store.apiBase)
            setTextColor(color(R.color.textPrimary))
            setHintTextColor(color(R.color.textMuted))
            background = Ui.pill(this@LoginActivity, R.color.bgInput)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        NxDialog(this)
            .title("Адрес сервера")
            .message("При старте приложение само читает актуальный туннель из nexus-redirect. Впишите адрес вручную, если авто-определение не сработало.")
            .view(input)
            .button("Сохранить") {
                val v = input.text.toString().trim()
                if (v.isNotEmpty()) {
                    Store.apiBase = v
                    Ui.snackbar(this, "Сервер: ${Store.apiBase}")
                }
            }
            .button("Отмена") {}
            .show()
    }
}