package com.nexus.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp

class IconPickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.bgPrimary, null))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(14), dp(16), dp(10))
        }
        val back = TextView(this).apply {
            text = "←"
            textSize = 24f
            setTextColor(resources.getColor(R.color.accentText, null))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        header.addView(back)
        header.addView(
            Ui.text(this, "Иконка приложения", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) }
        )
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(32))
        }

        val current = IconManager.getCurrent(this)

        val previewWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(16), dp(16), dp(16), dp(20))
            background = Ui.card(this@IconPickerActivity)
        }
        val bigPreview = ImageView(this).apply {
            setImageResource(current.drawable)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        previewWrap.addView(bigPreview, LinearLayout.LayoutParams(dp(96), dp(96)))
        previewWrap.addView(
            Ui.text(this, "Сейчас: ${current.displayName}", 14f, R.color.textSecondary),
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(10) }
        )
        content.addView(previewWrap, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        content.addView(
            Ui.text(this, "Выберите стиль — смена происходит сразу", 13f, R.color.accentText, true),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(20); bottomMargin = dp(8) }
        )

        val grid = GridLayout(this).apply { columnCount = 2 }

        IconManager.IconStyle.values().forEach { style ->
            val isActive = style == current
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = Ui.card(this@IconPickerActivity)
                setPadding(dp(12), dp(16), dp(12), dp(14))
            }

            val icon = ImageView(this).apply {
                setImageResource(style.drawable)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            cell.addView(icon, LinearLayout.LayoutParams(dp(72), dp(72)))

            cell.addView(
                Ui.text(this, style.displayName, 13f, R.color.textPrimary),
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(10) }
            )

            if (isActive) {
                cell.addView(
                    Ui.text(this, "✓ текущая", 11f, R.color.accentText),
                    LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(2) }
                )
            }

            cell.setOnClickListener {
                if (isActive) {
                    Ui.snackbar(this, "Уже выбрано")
                    return@setOnClickListener
                }
                // Без подтверждения: меняем сразу
                IconManager.setCurrent(this@IconPickerActivity, style)
                startActivity(Intent(this@IconPickerActivity, IconPickerActivity::class.java)
                    .putExtra("changed", style.displayName))
                finish()
                overridePendingTransition(0, 0)
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            }
            grid.addView(cell, params)
        }

        content.addView(grid, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        content.addView(
            Ui.text(this, "💡 Иконка может появиться в лаунчере через 2-5 секунд. Если не появилась — перезапустите лаунчер.", 12f, R.color.textMuted),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(20) }
        )

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)

        intent.getStringExtra("changed")?.let {
            Ui.snackbar(this, "Иконка «$it» изменена ✓")
        }
    }
}