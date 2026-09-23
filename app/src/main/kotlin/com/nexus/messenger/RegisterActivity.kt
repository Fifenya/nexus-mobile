package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.User
import org.json.JSONObject

class RegisterActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(64, 128, 64, 64)
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        }

        root.addView(TextView(this).apply {
            text = "Создать аккаунт"; textSize = 24f; gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 48 })

        val fields = mutableListOf<EditText>()
        listOf("Имя пользователя" to 1, "Email (необязательно)" to 1, "Пароль" to 0x81, "Повторите пароль" to 0x81).forEach { (h, t) ->
            val e = EditText(this).apply {
                hint = h; inputType = t
                setTextColor(resources.getColor(R.color.textPrimary, null))
                setHintTextColor(resources.getColor(R.color.textMuted, null))
                setBackgroundColor(resources.getColor(R.color.bgInput, null))
                setPadding(32, 24, 32, 24)
            }
            root.addView(e, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 24 })
            fields.add(e)
        }

        val btn = Button(this).apply {
            text = "Создать"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundColor(resources.getColor(R.color.accent, null))
        }
        root.addView(btn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 140))

        setContentView(root)

        btn.setOnClickListener {
            val (u, e, p, p2) = fields.map { it.text.toString() }
            if (u.isEmpty() || p.isEmpty()) {
                AlertDialog.Builder(this).setMessage("Заполните обязательные поля").setPositiveButton("OK", null).show()
                return@setOnClickListener
            }
            if (p != p2) {
                AlertDialog.Builder(this).setMessage("Пароли не совпадают").setPositiveButton("OK", null).show()
                return@setOnClickListener
            }
            btn.text = "Создание..."
            Api.post("/auth/register", JSONObject().put("username", u).put("email", e).put("password", p)) { code, body ->
                runOnUiThread {
                    btn.text = "Создать"
                    val j = Api.parseObj(body)
                    if ((code == 200 || code == 201) && j != null) {
                        Store.token = j.optString("access_token").takeIf { it.isNotEmpty() } ?: j.optString("token")
                        j.optJSONObject("user")?.let { Store.user = User.fromJson(it) }
                        startActivity(Intent(this, ChatsActivity::class.java))
                        finish()
                    } else {
                        AlertDialog.Builder(this).setTitle("Ошибка").setMessage(body).setPositiveButton("OK", null).show()
                    }
                }
            }
        }
    }
}