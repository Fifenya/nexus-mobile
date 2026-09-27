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

class PowerSaveActivity : Activity() {

    private lateinit var pollValue: TextView
    private lateinit var refreshValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }
        root.addView(header("Энергосбережение"))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        val modeCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = Ui.card(this@PowerSaveActivity)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val mid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(Ui.text(this, "Режим энергосбережения", 16f, R.color.textPrimary),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        mid.addView(Ui.text(this, "Реже опрашивать сервер, не обновлять списки фоном", 13f, R.color.textSecondary),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
        modeCard.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        modeCard.addView(Ui.switch(this, LocalPrefs.powerSave) { on ->
            LocalPrefs.powerSave = on
            refreshValues()
            Ui.snackbar(this, if (on) "Энергосбережение включено" else "Энергосбережение выключено")
        }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        content.addView(modeCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@PowerSaveActivity)
        }
        infoCard.addView(Ui.text(this, "Что меняется", 13f, R.color.accentText, true)
            .apply { setPadding(dp(16), dp(14), dp(16), dp(6)) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        pollValue = Ui.text(this, "", 15f, R.color.textPrimary)
        infoCard.addView(valueRow("Опрос статуса «в сети»", pollValue))
        infoCard.addView(View(this).apply { setBackgroundColor(color(R.color.divider)) },
            LinearLayout.LayoutParams(MATCH_PARENT, 1).apply { leftMargin = dp(16) })
        refreshValue = Ui.text(this, "", 15f, R.color.textPrimary)
        infoCard.addView(valueRow("Обновление списка чатов", refreshValue))
        content.addView(infoCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) })

        content.addView(Ui.text(this, "Сообщения по-прежнему доставляются при открытом чате. Режим влияет только на фоновую активность.", 12f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)
        refreshValues()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun refreshValues() {
        pollValue.text = if (LocalPrefs.powerSave) "каждые 60 сек" else "каждые 20 сек"
        refreshValue.text = if (LocalPrefs.powerSave) "только вручную" else "при каждом входе"
    }

    private fun valueRow(label: String, valueView: TextView): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(Ui.text(this@PowerSaveActivity, label, 15f, R.color.textSecondary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(valueView)
        }

    private fun header(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(color(R.color.bgSecondary))
        setPadding(dp(4), dp(10), dp(16), dp(10))
        val back = TextView(this@PowerSaveActivity).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        addView(back)
        addView(Ui.text(this@PowerSaveActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }
}