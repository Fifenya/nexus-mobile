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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LanguageActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }
        root.addView(header("Язык"))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        content.addView(card {
            addView(radioRow("Системный язык", "Как в настройках Android", !LocalPrefs.forceRu) {
                LocalPrefs.forceRu = false
                Ui.snackbar(this@LanguageActivity, "Язык: системный")
                recreate()
            })
            addView(dividerInset())
            addView(radioRow("Русский (всегда)", "Даты и подписи принудительно на русском", LocalPrefs.forceRu) {
                LocalPrefs.forceRu = true
                Ui.snackbar(this@LanguageActivity, "Язык: русский")
                recreate()
            })
        })

        val example = SimpleDateFormat("EEE, d MMM", LocalPrefs.formatLocale()).format(Date())
        content.addView(Ui.text(this, "Пример формата дат: $example", 13f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) })

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
        val back = TextView(this@LanguageActivity).apply {
            text = "←"; textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        addView(back)
        addView(Ui.text(this@LanguageActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }

    private fun card(build: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.card(this@LanguageActivity)
        build()
    }

    private fun radioRow(title: String, sub: String, active: Boolean, onClick: () -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val mid = LinearLayout(this@LanguageActivity).apply { orientation = LinearLayout.VERTICAL }
            mid.addView(Ui.text(this@LanguageActivity, title, 16f, R.color.textPrimary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            mid.addView(Ui.text(this@LanguageActivity, sub, 13f, R.color.textSecondary),
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
            addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            if (active) {
                addView(Ui.icon(this@LanguageActivity, R.drawable.ic_check, color(R.color.accentText)),
                    LinearLayout.LayoutParams(dp(22), dp(22)))
            }
            setOnClickListener { onClick() }
        }
}