package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class SettingsActivity : Activity() {
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
            text = "Настройки"; textSize = 20f
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 16 })
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val profile = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(32, 32, 32, 32)
        }
        val avatar = TextView(this).apply {
            text = (com.nexus.messenger.data.Store.user?.username ?: "?").take(1).uppercase()
            textSize = 24f; gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundColor(resources.getColor(R.color.accent, null))
        }
        profile.addView(avatar, LinearLayout.LayoutParams(120, 120))
        val pInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        pInfo.addView(TextView(this).apply {
            text = com.nexus.messenger.data.Store.user?.username ?: "Гость"; textSize = 18f
            setTextColor(resources.getColor(R.color.textPrimary, null))
        })
        pInfo.addView(TextView(this).apply {
            text = "Нажмите, чтобы изменить профиль"; textSize = 12f
            setTextColor(resources.getColor(R.color.textMuted, null))
        })
        profile.addView(pInfo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = 24 })
        profile.setOnClickListener { startActivity(Intent(this@SettingsActivity, ProfileActivity::class.java)) }  // ИСПРАВЛЕНО
        root.addView(profile, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 16 })

        val menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
        }
        listOf(
            "🤖 Боты" to BotsActivity::class.java,
            "🌟 Мотесы" to null,
            "🎨 Темы" to null,
            "🔒 Конфиденциальность" to null
        ).forEach { (label, cls) ->
            val item = TextView(this).apply {
                text = label; textSize = 16f; setPadding(32, 32, 32, 32)
                setTextColor(resources.getColor(R.color.textPrimary, null))
            }
            cls?.let { item.setOnClickListener { startActivity(Intent(this@SettingsActivity, it)) } }  // ИСПРАВЛЕНО
            menu.addView(item)
        }
        root.addView(menu, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 16 })

        val apiWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32)
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
        }
        apiWrap.addView(TextView(this).apply {
            text = "URL сервера"; textSize = 12f
            setTextColor(resources.getColor(R.color.textMuted, null))
        })
        val apiUrl = EditText(this).apply {
            setText(com.nexus.messenger.data.Store.apiBase); setTextColor(resources.getColor(R.color.textPrimary, null))
            setHintTextColor(resources.getColor(R.color.textMuted, null))
            setBackgroundColor(resources.getColor(R.color.bgInput, null))
            setPadding(24, 16, 24, 16)
        }
        apiWrap.addView(apiUrl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 })
        val saveApi = Button(this).apply {
            text = "Сохранить"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundColor(resources.getColor(R.color.accent, null))
        }
        saveApi.setOnClickListener {
            com.nexus.messenger.data.Store.apiBase = apiUrl.text.toString().trim()
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
        }
        apiWrap.addView(saveApi, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110).apply { topMargin = 12 })
        root.addView(apiWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 16 })

        val logout = Button(this).apply {
            text = "Выйти"; setTextColor(resources.getColor(R.color.textPrimary, null))
            setBackgroundColor(resources.getColor(R.color.danger, null))
        }
        logout.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("Выйти из аккаунта?")
                .setPositiveButton("Выйти") { _, _ ->
                    com.nexus.messenger.data.Store.logout()
                    startActivity(Intent(this@SettingsActivity, LoginActivity::class.java))
                    finishAffinity()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
        root.addView(logout, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 130).apply {
            setMargins(48, 48, 48, 0)
        })

        root.addView(TextView(this).apply {
            text = "Nexus Messenger v1.0.0"; textSize = 12f; gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.textMuted, null))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 32; bottomMargin = 32 })

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }
}