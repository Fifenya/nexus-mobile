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
import com.nexus.messenger.ui.NxDialog
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp

class PrivacySettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }
        root.addView(header("Конфиденциальность"))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        content.addView(Ui.text(this, "Контролируйте, кто видит вашу информацию в Nexus", 13f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(10) })

        content.addView(card {
            addView(sectionLabel("Кто видит..."))
            addView(privacyRow("Время захода и статус в сети", LocalPrefs.privacyOnline) { newVal ->
                LocalPrefs.privacyOnline = newVal
                Ui.snackbar(this@PrivacySettingsActivity, "Сохранено: $newVal")
            })
            addView(dividerInset())
            addView(privacyRow("Фото профиля", LocalPrefs.privacyPhoto) { newVal ->
                LocalPrefs.privacyPhoto = newVal
                Ui.snackbar(this@PrivacySettingsActivity, "Сохранено: $newVal")
            })
            addView(dividerInset())
            addView(privacyRow("Описание профиля", LocalPrefs.privacyBio) { newVal ->
                LocalPrefs.privacyBio = newVal
                Ui.snackbar(this@PrivacySettingsActivity, "Сохранено: $newVal")
            })
        })

        content.addView(card {
            addView(sectionLabel("Дополнительно"))
            addView(switchRow("Двухэтапная аутентификация", "Дополнительный пароль при входе", false) {
                Ui.snackbar(this@PrivacySettingsActivity, "Появится в следующем обновлении")
            })
            addView(dividerInset())
            addView(switchRow("Заблокированные пользователи", "Список контактов в чёрном списке", false) {
                Ui.snackbar(this@PrivacySettingsActivity, "Появится в следующем обновлении")
            })
        })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun header(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(color(R.color.bgSecondary))
        setPadding(dp(4), dp(10), dp(16), dp(10))
        val back = TextView(this@PrivacySettingsActivity).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        addView(back)
        addView(Ui.text(this@PrivacySettingsActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }

    private fun card(build: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.card(this@PrivacySettingsActivity)
        build()
    }

    private fun sectionLabel(s: String): TextView =
        Ui.text(this, s, 13f, R.color.accentText, true).apply { setPadding(dp(16), dp(14), dp(16), dp(6)) }

    private fun dividerInset() = android.view.View(this).apply { setBackgroundColor(color(R.color.divider)) }

    private fun privacyRow(title: String, currentValue: String, onChange: (String) -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(Ui.text(this@PrivacySettingsActivity, title, 16f, R.color.textPrimary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(Ui.text(this@PrivacySettingsActivity, currentValue, 14f, R.color.accentText))
            setOnClickListener {
                NxDialog(this@PrivacySettingsActivity).items(listOf("Все", "Мои контакты", "Никто")) { i ->
                    val newVal = when (i) {
                        0 -> "Все"
                        1 -> "Мои контакты"
                        else -> "Никто"
                    }
                    onChange(newVal)
                }.show()
            }
        }

    private fun switchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val mid = LinearLayout(this@PrivacySettingsActivity).apply { orientation = LinearLayout.VERTICAL }
            mid.addView(Ui.text(this@PrivacySettingsActivity, title, 16f, R.color.textPrimary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            mid.addView(Ui.text(this@PrivacySettingsActivity, sub, 13f, R.color.textSecondary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
            addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(Ui.switch(this@PrivacySettingsActivity, checked, onChange),
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
}