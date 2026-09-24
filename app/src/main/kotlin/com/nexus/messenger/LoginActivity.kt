package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.User
import com.nexus.messenger.ui.dp
import org.json.JSONObject

class LoginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(64), dp(32), dp(32))
        }
        scroll.addView(root, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // ─── Логотип ───
        val logo = TextView(this).apply {
            text = "N"
            textSize = 44f
            setTextColor(resources.getColor(R.color.textPrimary, null))
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_logo)
            elevation = dp(8).toFloat()
        }
        root.addView(logo, LinearLayout.LayoutParams(dp(100), dp(100)).apply {
            gravity = Gravity.CENTER
            bottomMargin = dp(24)
        })

        // ─── Заголовок ───
        root.addView(TextView(this).apply {
            text = "Nexus"
            textSize = 32f
            setTextColor(resources.getColor(R.color.textPrimary, null))
            gravity = Gravity.CENTER
            paintFlags = paintFlags or android.graphics.Paint.ANTI_ALIAS_FLAG
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        root.addView(TextView(this).apply {
            text = "Защищённый мессенджер"
            textSize = 15f
            setTextColor(resources.getColor(R.color.textSecondary, null))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(6)
            bottomMargin = dp(48)
        })

        // ─── Поле: Имя пользователя ───
        val username = EditText(this).apply {
            hint = "Имя пользователя"
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundResource(R.drawable.bg_input)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            textSize = 16f
            singleLine = true
        }
        root.addView(username, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            bottomMargin = dp(12)
        })

        // ─── Поле: Пароль ───
        val password = EditText(this).apply {
            hint = "Пароль"
            inputType = 0x00000081
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundResource(R.drawable.bg_input)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            textSize = 16f
            singleLine = true
        }
        root.addView(password, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            bottomMargin = dp(24)
        })

        // ─── Кнопка: Войти ───
        val btn = Button(this).apply {
            text = "Войти"
            textSize = 16f
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundResource(R.drawable.bg_button_accent)
            isAllCaps = false
            stateListAnimator = null
            elevation = dp(4).toFloat()
        }
        root.addView(btn, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)))

        // ─── Ссылка: Регистрация ───
        val link = TextView(this).apply {
            text = "Нет аккаунта? Создать"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(resources.getColor(R.color.textSecondary, null))
            setPadding(dp(16), dp(20), dp(16), dp(16))
        }
        link.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }
        root.addView(link, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // ─── Версия ───
        root.addView(TextView(this).apply {
            text = "Nexus Messenger v1.0.0"
            gravity = Gravity.CENTER
            textSize = 11f
            setTextColor(resources.getColor(R.color.textMuted, null))
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(32)
        })

        setContentView(scroll)

        // ─── Логика ───
        btn.setOnClickListener {
            val u = username.text.toString().trim()
            val p = password.text.toString()
            if (u.isEmpty() || p.isEmpty()) {
                AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setMessage("Заполните все поля")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
            btn.text = "Вход..."
            btn.isEnabled = false
            Api.post("/auth/login", JSONObject().put("username", u).put("password", p)) { code, body ->
                runOnUiThread {
                    btn.text = "Войти"
                    btn.isEnabled = true
                    if (code == 200 || code == 201) {
                        val j = Api.parseObj(body)
                        if (j != null) {
                            Store.token = j.optString("access_token").takeIf { it.isNotEmpty() }
                                ?: j.optString("token")
                            j.optJSONObject("user")?.let { Store.user = User.fromJson(it) }
                            startActivity(Intent(this, ChatsActivity::class.java))
                            finish()
                            return@runOnUiThread
                        }
                    }
                    AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                        .setTitle("Ошибка входа")
                        .setMessage(body)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }
}