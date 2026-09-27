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
        root.addView(header("Устройства"))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        content.addView(card {
            addView(sectionLabel("Это устройство"))
            addView(infoRow("Устройство", "${Build.MANUFACTURER} ${Build.MODEL}"))
            addView(dividerInset())
            addView(infoRow("Система", "Android ${Build.VERSION.RELEASE}"))
            addView(dividerInset())
            addView(infoRow("Приложение", "Nexus ${appVersion()}"))
            addView(dividerInset())
            addView(infoRow("Статус", "в сети", valueColor = R.color.online))
        })

        content.addView(card {
            addView(valueRow("Завершить сеанс") {
                NxDialog(this@DevicesActivity)
                    .message("Выйти из аккаунта на этом устройстве?")
                    .button("Выйти") {
                        Store.logout()
                        startActivity(Intent(this@DevicesActivity, LoginActivity::class.java))
                        finishAffinity()
                    }
                    .button("Отмена") {}
                    .show()
            })
        })

        content.addView(Ui.text(this, "Другие сеансы появятся здесь, когда на сервере будет включена мульти-девайс поддержка.", 12f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun color(res: Int): Int = resources.getColor(res, null)
    private fun dividerInset() = android.view.View(this).apply { setBackgroundColor(color(R.color.divider)) }

    private fun appVersion(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) { "1.0.0" }

    private fun header(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(color(R.color.bgSecondary))
        setPadding(dp(4), dp(10), dp(16), dp(10))
        val back = TextView(this@DevicesActivity).apply {
            text = "←"; textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        addView(back)
        addView(Ui.text(this@DevicesActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }

    private fun card(build: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.card(this@DevicesActivity)
        build()
    }

    private fun sectionLabel(s: String): TextView =
        Ui.text(this, s, 13f, R.color.accentText, true).apply { setPadding(dp(16), dp(14), dp(16), dp(6)) }

    private fun infoRow(label: String, value: String, valueColor: Int = R.color.textPrimary): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(Ui.text(this@DevicesActivity, label, 15f, R.color.textSecondary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(Ui.text(this@DevicesActivity, value, 15f, valueColor))
        }

    private fun valueRow(title: String, onClick: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(Ui.text(this@DevicesActivity, title, 16f, R.color.danger),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            setOnClickListener { onClick() }
        }
