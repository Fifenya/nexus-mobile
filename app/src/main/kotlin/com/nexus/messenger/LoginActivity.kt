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
import org.json.JSONObject

class LoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 128, 64, 64)
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        }

        // Логотип
        val logo = TextView(this).apply {
            text = "N"
            textSize = 56f
            setTextColor(resources.getColor(R.color.textPrimary, null))
            gravity = Gravity.CENTER
            setBackgroundColor(resources.getColor(R.color.accent, null))
        }
        root.addView(logo, LinearLayout.LayoutParams(240, 240).apply { gravity = Gravity.CENTER })

        root.addView(TextView(this).apply {
            text = "Nexus"; textSize = 28f; gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = 32 })

        root.addView(TextView(this).apply {
            text = "Защищённый мессенджер"; textSize = 14f; gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textSecondary, null))
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = 8; bottomMargin = 64 })

        val username = EditText(this).apply {
            hint = "Имя пользователя"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundColor(resources.getColor(R.color.bgInput, null))
            setPadding(32, 24, 32, 24)
        }
        root.addView(username, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = 24 })

        val password = EditText(this).apply {
            hint = "Пароль"; inputType = 0x00000081 // textPassword
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundColor(resources.getColor(R.color.bgInput, null))
            setPadding(32, 24, 32, 24)
        }
        root.addView(password, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = 32 })

        val btn = Button(this).apply {
            text = "Войти"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundColor(resources.getColor(R.color.accent, null))
        }
        root.addView(btn, LinearLayout.LayoutParams(MATCH_PARENT, 140))

        val link = TextView(this).apply {
            text = "Нет аккаунта? Создать"; gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.accent, null))
        }
        link.setOnClickListener { startActivity(Intent(this@LoginActivity, RegisterActivity::class.java)) }
        root.addView(link, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = 32 })

        setContentView(root)

        btn.setOnClickListener {
            val u = username.text.toString().trim()
            val p = password.text.toString()
            if (u.isEmpty() || p.isEmpty()) {
                AlertDialog.Builder(this).setMessage("Заполните все поля").setPositiveButton("OK", null).show()
                return@setOnClickListener
            }
            btn.text = "Вход..."
            btn.isEnabled = false
            Api.post("/auth/login", JSONObject().put("username", u).put("password", p)) { code, body ->
                runOnUiThread {
                    btn.text = "Войти"; btn.isEnabled = true
                    if (code == 200 || code == 201) {
                        val j = Api.parseObj(body)
                        if (j != null) {
                            Store.token = j.optString("access_token").takeIf { it.isNotEmpty() }
                                ?: j.optString("token")
                            val userObj = j.optJSONObject("user")
                            if (userObj != null) Store.user = User.fromJson(userObj)
                            startActivity(Intent(this, ChatsActivity::class.java))
                            finish()
                            return@runOnUiThread
                        }
                    }
                    AlertDialog.Builder(this).setTitle("Ошибка входа").setMessage(body).setPositiveButton("OK", null).show()
                }
            }
        }
    }
}