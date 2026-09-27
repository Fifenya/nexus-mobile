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

class DataStorageActivity : Activity() {
    private lateinit var cacheValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }
        root.addView(header("Данные и память"))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        cacheValue = Ui.text(this, LocalPrefs.humanSize(LocalPrefs.cacheSize(this)), 14f, R.color.accentText)

        content.addView(card {
            addView(sectionLabel("Использование памяти"))
            addView(valueRow("Кэш сообщений и чатов", cacheValue) {})
            addView(dividerInset())
            addView(valueRow("Очистить кэш", null) {
                NxDialog(this@DataStorageActivity)
                    .message("Удалить кэш чатов и сообщений? Данные останутся на сервере.")
                    .button("Очистить") {
                        LocalPrefs.clearCache(this@DataStorageActivity)
                        cacheValue.text = LocalPrefs.humanSize(LocalPrefs.cacheSize(this@DataStorageActivity))
                        Ui.snackbar(this@DataStorageActivity, "Кэш очищен")
                    }
                    .button("Отмена") {}
                    .show()
            }, danger = true)
        })

        content.addView(card {
            addView(sectionLabel("Автозагрузка медиа"))
            addView(switchRow("Через мобильную сеть", "Фото и видео", false) {
                Ui.snackbar(this@DataStorageActivity, "Вложения появятся позже — настройка сохранена")
            })
            addView(dividerInset())
            addView(switchRow("Через Wi-Fi", "Фото и видео", true) {
                Ui.snackbar(this@DataStorageActivity, "Вложения появятся позже — настройка сохранена")
            })
        })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun color(res: Int): Int = resources.getColor(res, null)
    private fun dividerInset() = android.view.View(this).apply { setBackgroundColor(color(R.color.divider)) }

    private fun header(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(color(R.color.bgSecondary))
        setPadding(dp(4), dp(10), dp(16), dp(10))
        val back = TextView(this@DataStorageActivity).apply {
            text = "←"; textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        addView(back)
        addView(Ui.text(this@DataStorageActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }

    private fun card(build: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.card(this@DataStorageActivity)
        build()
    }

    private fun sectionLabel(s: String): TextView =
        Ui.text(this, s, 13f, R.color.accentText, true).apply { setPadding(dp(16), dp(14), dp(16), dp(6)) }

    private fun valueRow(title: String, valueView: TextView?, onClick: () -> Unit, danger: Boolean = false): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(Ui.text(this@DataStorageActivity, title, 16f, if (danger) R.color.danger else R.color.textPrimary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            valueView?.let { addView(it) }
            setOnClickListener { onClick() }
        }

    private fun switchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val mid = LinearLayout(this@DataStorageActivity).apply { orientation = LinearLayout.VERTICAL }
            mid.addView(Ui.text(this@DataStorageActivity, title, 16f, R.color.textPrimary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            mid.addView(Ui.text(this@DataStorageActivity, sub, 13f, R.color.textSecondary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
            addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(Ui.switch(this@DataStorageActivity, checked, onChange),
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
}