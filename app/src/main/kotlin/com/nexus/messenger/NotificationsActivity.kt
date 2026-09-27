package com.nexus.messenger

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.data.LocalPrefs
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp

class NotificationsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }
        root.addView(header("Уведомления и звуки"))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        content.addView(Ui.text(this, "Push-уведомления появятся в следующем обновлении. Эти переключатели управляют событиями внутри приложения.", 12f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(10) })

        content.addView(card {
            addView(switchRow("Уведомления из чатов", "Показывать события новых сообщений", LocalPrefs.notifEnabled) { LocalPrefs.notifEnabled = it })
            addView(dividerInset())
            addView(switchRow("Звук", "Короткий сигнал при новом сообщении", LocalPrefs.notifSound) { LocalPrefs.notifSound = it })
            addView(dividerInset())
            addView(switchRow("Вибросигнал", "Вибрация при новом сообщении", LocalPrefs.notifVibro) { LocalPrefs.notifVibro = it })
            addView(dividerInset())
            addView(switchRow("Показывать текст", "Текст сообщения в уведомлении", LocalPrefs.notifPreview) { LocalPrefs.notifPreview = it })
        })

        content.addView(card {
            addValueRow("Рингтон", "Стандартный") {
                Ui.snackbar(this@NotificationsActivity, "Выбор рингтона появится позже")
            }
        })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun color(res: Int): Int = resources.getColor(res, null)
    private fun dividerInset() = android.view.View(this).apply {
        setBackgroundColor(color(R.color.divider))
    }.also { it.setPadding(0, 0, 0, 0) }

    private fun header(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(color(R.color.bgSecondary))
        setPadding(dp(4), dp(10), dp(16), dp(10))
        val back = TextView(this@NotificationsActivity).apply {
            text = "←"; textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        addView(back)
        addView(Ui.text(this@NotificationsActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }

    private fun card(build: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.card(this@NotificationsActivity)
        build()
    }

    private fun switchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val mid = LinearLayout(this@NotificationsActivity).apply { orientation = LinearLayout.VERTICAL }
            mid.addView(Ui.text(this@NotificationsActivity, title, 16f, R.color.textPrimary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            mid.addView(Ui.text(this@NotificationsActivity, sub, 13f, R.color.textSecondary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
            addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(Ui.switch(this@NotificationsActivity, checked, onChange),
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }

    private fun addValueRow(title: String, value: String, onClick: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(Ui.text(this@NotificationsActivity, title, 16f, R.color.textPrimary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(Ui.text(this@NotificationsActivity, value, 14f, R.color.accentText))
            setOnClickListener { onClick() }
        }
}