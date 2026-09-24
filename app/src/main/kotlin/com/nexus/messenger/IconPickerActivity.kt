package com.nexus.messenger

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
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
            setBackgroundColor(resources.getColor(R.color.bgSecondary, null))
            setPadding(dp(16), dp(32), dp(16), dp(16))
        }
        val back = TextView(this).apply {
            text = "←"
            textSize = 24f
            setTextColor(resources.getColor(R.color.accent, null))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        header.addView(back)
        header.addView(TextView(this).apply {
            text = "Иконка приложения"
            textSize = 20f
            setTextColor(resources.getColor(R.color.textPrimary, null))
        }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(16) })
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }

        content.addView(TextView(this).apply {
            text = "Выберите стиль иконки в лаунчере"
            textSize = 14f
            setTextColor(resources.getColor(R.color.textSecondary, null))
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(16) })

        val current = IconManager.getCurrent(this)

        val grid = GridLayout(this).apply {
            columnCount = 2
            useDefaultMargins = true
        }

        IconManager.IconStyle.values().forEach { style ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.icon_selector)
                setPadding(dp(12), dp(16), dp(12), dp(16))
                isSelected = (style == current)
            }

            val iconPreview = ImageView(this).apply {
                setImageResource(style.drawable)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            cell.addView(iconPreview, LinearLayout.LayoutParams(dp(80), dp(80)))

            cell.addView(TextView(this).apply {
                text = style.displayName
                textSize = 13f
                setTextColor(resources.getColor(R.color.textPrimary, null))
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(10) })

            if (style == current) {
                cell.addView(TextView(this).apply {
                    text = "✓ текущая"
                    textSize = 11f
                    setTextColor(resources.getColor(R.color.accent, null))
                }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(4) })
            }

            cell.setOnClickListener {
                if (style == current) {
                    Toast.makeText(this@IconPickerActivity, "Уже выбрано", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                    .setTitle("Сменить иконку?")
                    .setMessage("Иконка \"${style.displayName}\" появится в лаунчере через несколько секунд.")
                    .setPositiveButton("Сменить") { _, _ ->
                        IconManager.setCurrent(this@IconPickerActivity, style)
                        Toast.makeText(this@IconPickerActivity, "Иконка изменена ✓", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
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

        content.addView(TextView(this).apply {
            text = "💡 Совет: после смены иконка появится в лаунчере через 2-5 секунд. Если не появилась — перезапустите лаунчер."
            textSize = 12f
            setTextColor(resources.getColor(R.color.textMuted, null))
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(24) })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        setContentView(root)
    }
}
