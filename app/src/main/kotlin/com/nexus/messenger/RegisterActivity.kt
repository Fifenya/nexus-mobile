package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
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

class RegisterActivity : Activity() {

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

        // Заголовок
        root.addView(TextView(this).apply {
            text = "Создать аккаунт"
            textSize = 26f
            setTextColor(resources.getColor(R.color.textPrimary, null))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            bottomMargin = dp(8)
        })

        root.addView(TextView(this).apply {
            text = "Присоединяйтесь к Nexus"
            textSize = 15f
            setTextColor(resources.getColor(R.color.textSecondary, null))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            bottomMargin = dp(40)
        })

        // Поля
        val fields = mutableListOf<EditText>()
        val hints = listOf("Имя пользователя", "Email (необязательно)", "Пароль", "Повторите пароль")
        val types = listOf(1, 1, 0x81, 0x81)

        hints.forEachIndexed { i, hint ->
            val e = EditText(this).apply {
                this.hint = hint
                inputType = types[i]
                setTextColor(resources.getColor(R.color.textPrimary, null))
                setHintTextColor(resources.getColor(R.color.textMuted, null))
                setBackgroundResource(R.drawable.bg_input)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                textSize = 16f
                singleLine = true
            }
            root.addView(e, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            })
            fields.add(e)
        }

        // Кнопка
        val btn = Button(this).apply {
            text = "Создать аккаунт"
            textSize = 16f
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundResource(R.drawable.bg_button_accent)
            isAllCaps = false
            stateListAnimator = null
            elevation = dp(4).toFloat()
        }
        root.addView(btn, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)).apply {
            topMargin = dp(12)
        })

        // Ссылка назад
        val link = TextView(this).apply {
            text = "Уже есть аккаунт? Войти"
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(resources.getColor(R.color.textSecondary, null))
            setPadding(dp(16), dp(20), dp(16), dp(16))
        }
        link.setOnClickListener { finish() }
        root.addView(link, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        setContentView(scroll)

        btn.setOnClickListener {
            val (u, e, p, p2) = fields.map { it.text.toString() }
            if (u.isEmpty() || p.isEmpty()) {
                AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setMessage("Заполните обязательные поля").setPositiveButton("OK", null).show()
                return@setOnClickListener
            }
            if (p != p2) {
                AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setMessage("Пароли не совпадают").setPositiveButton("OK", null).show()
                return@setOnClickListener
            }
            btn.text = "Создание..."
            btn.isEnabled = false
            Api.post("/auth/register", JSONObject().put("username", u).put("email", e).put("password", p)) { code, body ->
                runOnUiThread {
                    btn.text = "Создать аккаунт"
                    btn.isEnabled = true
                    val j = Api.parseObj(body)
                    if ((code == 200 || code == 201) && j != null) {
                        Store.token = j.optString("access_token").takeIf { it.isNotEmpty() } ?: j.optString("token")
                        j.optJSONObject("user")?.let { Store.user = User.fromJson(it) }
                        startActivity(Intent(this, ChatsActivity::class.java))
                        finish()
                    } else {
                        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                            .setTitle("Ошибка").setMessage(body).setPositiveButton("OK", null).show()
                    }
                }
            }
        }
    }
}