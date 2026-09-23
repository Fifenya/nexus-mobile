package com.nexus.messenger

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.User
import org.json.JSONObject

class ProfileActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(16, 32, 16, 32)
        }
        val back = Button(this).apply {
            text = "←"; setBackgroundColor(0); setTextColor(resources.getColor(R.color.accent, null)); textSize = 22f
        }
        back.setOnClickListener { finish() }
        header.addView(back)
        header.addView(TextView(this).apply {
            text = "Профиль"; textSize = 20f; setTextColor(resources.getColor(R.color.textPrimary, null))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 16 })
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val avatar = TextView(this).apply {
            text = (Store.user?.username ?: "?").take(1).uppercase()
            textSize = 36f; gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundColor(resources.getColor(R.color.accent, null))
        }
        root.addView(avatar, LinearLayout.LayoutParams(200, 200).apply { gravity = Gravity.CENTER; topMargin = 48 })

        root.addView(TextView(this).apply {
            text = Store.user?.username ?: "—"; textSize = 22f; gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 24 })

        val displayName = EditText(this).apply {
            hint = "Отображаемое имя"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundColor(resources.getColor(R.color.bgInput, null))
            setPadding(32, 24, 32, 24)
        }
        root.addView(displayName, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(48, 48, 48, 0)
        })

        val save = Button(this).apply {
            text = "Сохранить"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundColor(resources.getColor(R.color.accent, null))
        }
        save.setOnClickListener {
            Api.patch("/users/me", JSONObject().put("displayName", displayName.text.toString())) { code, body ->
                runOnUiThread {
                    if (code == 200) {
                        val j = Api.parseObj(body)
                        if (j != null) Store.user = User.fromJson(j)
                        Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        AlertDialog.Builder(this).setMessage(body).setPositiveButton("OK", null).show()
                    }
                }
            }
        }
        root.addView(save, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 130).apply { setMargins(48, 24, 48, 0) })

        setContentView(root)

        Api.get("/users/me") { code, body ->
            runOnUiThread {
                if (code == 200) {
                    val j = Api.parseObj(body)
                    if (j != null) {
                        displayName.setText(j.optString("displayName"))
                    }
                }
            }
        }
    }
}