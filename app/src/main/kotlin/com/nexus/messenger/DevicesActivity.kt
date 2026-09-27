package com.nexus.messenger

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.data.Store
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp

class DevicesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }
        root.addView(headerView("Устройства"), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        val infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@DevicesActivity)
        }
        infoCard.addView(sectionLabel("Это устройство"))
        infoCard.addView(infoRow("Устройство", "${Build.MANUFACTURER} ${Build.MODEL}"))
        infoCard.addView(dividerView(), LinearLayout.LayoutParams(MATCH_PARENT, 1))
        infoCard.addView(infoRow("Система", "Android ${Build.VERSION.RELEASE}"))
        infoCard.addView(dividerView(), LinearLayout.LayoutParams(MATCH_PARENT, 1))
        infoCard.addView(infoRow("Приложение", "Nexus ${appVersion()}"))
        infoCard.addView(dividerView(), LinearLayout.LayoutParams(MATCH_PARENT, 1))
        infoCard.addView(infoRow("Статус", "в сети", R.color.online))
        content.addView(infoCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val sessionCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@DevicesActivity)
        }
        val logoutRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        logoutRow.addView(Ui.text(this, "Завершить сеанс", 16f, R.color.danger),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        logoutRow.setOnClickListener {
            NxDialog(this)
                .message("Выйти из аккаунта на этом устройстве?")
                .button("Выйти") {
                    Store.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .button("Отмена") {}
                .show()
        }
        sessionCard.addView(logoutRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        content.addView(sessionCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })

        val note = Ui.text(this, "Другие сеансы появятся здесь, когда на сервере будет включена мульти-девайс поддержка.", 12f, R.color.textMuted)
        content.addView(note, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) })

        scroll.addView(content, ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun dividerView() = android.view.View(this).apply { setBackgroundColor(color(R.color.divider)) }

    private fun sectionLabel(s: String): TextView =
        Ui.text(this, s, 13f, R.color.accentText, true).apply { setPadding(dp(16), dp(14), dp(16), dp(6)) }

    private fun appVersion(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    private fun headerView(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(color(R.color.bgSecondary))
        setPadding(dp(4), dp(10), dp(16), dp(10))
        val back = TextView(this@DevicesActivity).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        addView(back)
        addView(Ui.text(this@DevicesActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }

    private fun infoRow(label: String, value: String, valueColor: Int = R.color.textPrimary): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(Ui.text(this@DevicesActivity, label, 15f, R.color.textSecondary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(Ui.text(this@DevicesActivity, value, 15f, valueColor))
        }
}