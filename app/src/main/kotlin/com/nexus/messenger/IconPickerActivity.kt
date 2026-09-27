package com.nexus.messenger

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp

class IconPickerActivity : Activity() {

    private lateinit var currentLabel: TextView
    private lateinit var preview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(color(R.color.bgSecondary))
            setPadding(dp(4), dp(10), dp(16), dp(10))
        }
        val back = TextView(this).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        header.addView(back)
        header.addView(Ui.text(this, "Иконка приложения", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val previewWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(32), dp(16), dp(16))
        }
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(IconManager.getCurrent(this@IconPickerActivity).drawable)
        }
        previewWrap.addView(preview, FrameLayout.LayoutParams(dp(110), dp(110)))
        currentLabel = Ui.text(this, IconManager.getCurrent(this).name, 18f, R.color.textPrimary, true)
        previewWrap.addView(currentLabel, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            topMargin = dp(16)
        })
        root.addView(previewWrap, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(32))
        }
        list.addView(Ui.text(this, "Выберите иконку", 13f, R.color.accentText, true)
            .apply { setPadding(dp(4), dp(8), dp(4), dp(6)) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@IconPickerActivity)
        }
        for ((i, opt) in IconManager.options.withIndex()) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(10), dp(16), dp(10))
            }
            val thumb = ImageView(this).apply {
                setImageResource(opt.drawable)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            row.addView(thumb, LinearLayout.LayoutParams(dp(48), dp(48)))
            row.addView(Ui.text(this, opt.name, 16f, R.color.textPrimary),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
            row.setOnClickListener {
                IconManager.setIcon(this, opt)
                preview.setImageResource(opt.drawable)
                currentLabel.text = opt.name
                Ui.snackbar(this, "Иконка «${opt.name}» выбрана — изменения появятся в лаунчере через минуту")
            }
            card.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            if (i < IconManager.options.size - 1) {
                card.addView(android.view.View(this).apply { setBackgroundColor(color(R.color.divider)) },
                    LinearLayout.LayoutParams(MATCH_PARENT, 1).apply { leftMargin = dp(80) })
            }
        }
        list.addView(card, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        list.addView(Ui.text(this, "Лаунчер может показывать новую иконку с задержкой — это ограничение Android.", 12f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) })

        scroll.addView(list, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun color(res: Int): Int = resources.getColor(res, null)
}