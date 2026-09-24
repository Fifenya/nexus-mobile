package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import com.nexus.messenger.ui.dp

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        }

        // Шапка
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(dp(16), dp(32), dp(16), dp(16))
        }
        val back = TextView(this).apply {
            text = "←"
            textSize = 24f
            setTextColor(resources.getColor(R.color.accent, null))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        header.addView(back)
        header.addView(TextView(this).apply {
            text = "Настройки"
            textSize = 20f
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        // Профиль
        val profile = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(dp(32), dp(32), dp(32), dp(32))
        }
        val avatar = TextView(this).apply {
            text = (com.nexus.messenger.data.Store.user?.username ?: "?").take(1).uppercase()
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundResource(R.drawable.bg_avatar)
        }
        profile.addView(avatar, LinearLayout.LayoutParams(dp(120), dp(120)))
        val pInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        pInfo.addView(TextView(this).apply {
            text = com.nexus.messenger.data.Store.user?.username ?: "Гость"
            textSize = 18f
            setTextColor(resources.getColor(R.color.textPrimary, null))
        })
        pInfo.addView(TextView(this).apply {
            text = "Нажмите, чтобы изменить профиль"
            textSize = 12f
            setTextColor(resources.getColor(R.color.textMuted, null))
        })
        profile.addView(pInfo, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(24) })
        profile.setOnClickListener {
            startActivity(Intent(this@SettingsActivity, ProfileActivity::class.java))
        }
        root.addView(profile, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) })

        // Меню
        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
        }

        val menuItems = listOf(
            "🎨 Иконка" to IconPickerActivity::class.java as Class<*>?,
            "🤖 Боты" to BotsActivity::class.java as Class<*>?,
            "🌟 Моты" to null,
            "🎨 Темы" to null,
            "🔒 Конфиденциальность" to null
        )

        for (menuItem in menuItems) {
            val label = menuItem.first
            val cls = menuItem.second
            val item = TextView(this).apply {
                text = label
                textSize = 16f
                setPadding(dp(32), dp(32), dp(32), dp(32))
                setTextColor(resources.getColor(R.color.textPrimary, null))
            }
            if (cls != null) {
                item.setOnClickListener {
                    startActivity(Intent(this@SettingsActivity, cls))
                }
            }
            menu.addView(item)
        }
        root.addView(menu, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) })

        // URL сервера
        val apiWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(32), dp(32), dp(32), dp(32))
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
        }
        apiWrap.addView(TextView(this).apply {
            text = "URL сервера"
            textSize = 12f
            setTextColor(resources.getColor(R.color.textMuted, null))
        })
        val apiUrl = EditText(this).apply {
            setText(com.nexus.messenger.data.Store.apiBase)
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundResource(R.drawable.bg_input)
            setPadding(dp(24), dp(16), dp(24), dp(16))
        }
        apiWrap.addView(apiUrl, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(8) })
        val saveApi = TextView(this).apply {
            text = "Сохранить"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundResource(R.drawable.bg_button_accent)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        saveApi.setOnClickListener {
            com.nexus.messenger.data.Store.apiBase = apiUrl.text.toString().trim()
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
        }
        apiWrap.addView(saveApi, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)).apply { topMargin = dp(12) })
        root.addView(apiWrap, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(16) })

        // Выход
        val logout = TextView(this).apply {
            text = "Выйти"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundResource(R.drawable.bg_button_danger)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        logout.setOnClickListener {
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setMessage("Выйти из аккаунта?")
                .setPositiveButton("Выйти") { _, _ ->
                    com.nexus.messenger.data.Store.logout()
                    startActivity(Intent(this@SettingsActivity, LoginActivity::class.java))
                    finishAffinity()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
        root.addView(logout, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)).apply {
            setMargins(dp(48), dp(48), dp(48), 0)
        })

        root.addView(TextView(this).apply {
            text = "Nexus Messenger v1.0.0"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textMuted, null))
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dp(32)
            bottomMargin = dp(32)
        })

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }
}
