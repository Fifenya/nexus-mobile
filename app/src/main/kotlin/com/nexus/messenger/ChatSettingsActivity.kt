package com.nexus.messenger

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.nexus.messenger.data.LocalPrefs
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.Wallpaper
import com.nexus.messenger.ui.dp

class ChatSettingsActivity : Activity() {

    private lateinit var previewWrap: LinearLayout
    private lateinit var previewBubble: TextView
    private lateinit var sizeValue: TextView
    private lateinit var radiusValue: TextView
    private val swatches = mutableListOf<TextView>()
    private val patternChips = mutableListOf<TextView>()
    private val patternNames = listOf("Нет", "Точки", "Диагонали", "Круги")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }
        root.addView(headerView("Настройки чатов"), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }
        scroll.addView(content, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)

        content.addView(buildPreview(), cardLp())
        content.addView(buildSizeCard(), cardLp())
        content.addView(buildRadiusCard(), cardLp())
        content.addView(buildColorCard(), cardLp())
        content.addView(buildPatternCard(), cardLp())
        content.addView(buildNamesCard(), cardLp())

        refreshSwatches()
        refreshPatternChips()
        applyPreview()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun cardLp() = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(10) }

    private fun cardView(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.card(this@ChatSettingsActivity)
    }

    private fun rowTitle(s: String): TextView =
        Ui.text(this, s, 13f, R.color.accentText, true).apply { setPadding(dp(16), dp(14), dp(16), dp(6)) }

    private fun headerView(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(color(R.color.bgSecondary))
        setPadding(dp(4), dp(10), dp(16), dp(10))
        val back = TextView(this@ChatSettingsActivity).apply {
            text = "←"
            textSize = 24f
            setTextColor(color(R.color.accentText))
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        back.setOnClickListener { finish() }
        addView(back)
        addView(Ui.text(this@ChatSettingsActivity, title, 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
    }

    private fun buildPreview(): LinearLayout {
        previewWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(20), dp(16), dp(20))
        }
        previewBubble = TextView(this).apply {
            text = "Доброе утро! 👋"
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(12), dp(8), dp(12), dp(8))
            gravity = Gravity.END
        }
        previewWrap.addView(previewBubble, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            gravity = Gravity.END
        })
        return previewWrap
    }

    private fun seekBar(progress: Int, max: Int, onChange: (Int) -> Unit): SeekBar = SeekBar(this).apply {
        this.max = max
        this.progress = progress
        progressTintList = ColorStateList.valueOf(color(R.color.accent))
        thumbTintList = ColorStateList.valueOf(color(R.color.accent))
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, from: Boolean) {
                if (from) onChange(p)
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    private fun buildSizeCard(): LinearLayout {
        val card = cardView()
        card.addView(rowTitle("Размер текста сообщений"))
        sizeValue = Ui.text(this, "${LocalPrefs.chatTextSize}", 14f, R.color.accentText, true)
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(12))
        }
        val seek = seekBar(LocalPrefs.chatTextSize - 12, 8) { p ->
            LocalPrefs.chatTextSize = 12 + p
            sizeValue.text = "${12 + p}"
            applyPreview()
        }
        line.addView(seek, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        line.addView(sizeValue, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dp(12) })
        card.addView(line, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return card
    }

    private fun buildRadiusCard(): LinearLayout {
        val card = cardView()
        card.addView(rowTitle("Углы блоков с сообщениями"))
        radiusValue = Ui.text(this, "${LocalPrefs.chatRadius}", 14f, R.color.accentText, true)
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(12))
        }
        val seek = seekBar(LocalPrefs.chatRadius, 24) { p ->
            LocalPrefs.chatRadius = p
            radiusValue.text = "$p"
            applyPreview()
        }
        line.addView(seek, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        line.addView(radiusValue, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { leftMargin = dp(12) })
        card.addView(line, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return card
    }

    private fun buildColorCard(): LinearLayout {
        val card = cardView()
        card.addView(rowTitle("Цвет фона чата"))
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(8), dp(16), dp(16))
        }
        LocalPrefs.chatBgColors.forEachIndexed { i, _ ->
            val sw = TextView(this)
            sw.setOnClickListener {
                LocalPrefs.chatBgIndex = i
                refreshSwatches()
                applyPreview()
            }
            swatches.add(sw)
            row.addView(sw, LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(12) })
        }
        card.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return card
    }

    private fun buildPatternCard(): LinearLayout {
        val card = cardView()
        card.addView(rowTitle("Обои: рисунок"))
        val chipsScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(4), dp(16), dp(16))
        }
        patternNames.forEachIndexed { i, name ->
            val chip = TextView(this).apply { text = name }
            chip.setOnClickListener {
                LocalPrefs.wallpaperPattern = i
                refreshPatternChips()
                applyPreview()
            }
            patternChips.add(chip)
            chipsRow.addView(chip, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { rightMargin = dp(8) })
        }
        chipsScroll.addView(chipsRow, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        card.addView(chipsScroll, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return card
    }

    private fun buildNamesCard(): LinearLayout {
        val card = cardView()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val mid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mid.addView(Ui.text(this@ChatSettingsActivity, "Имена отправителей в группах", 16f, R.color.textPrimary),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        mid.addView(Ui.text(this@ChatSettingsActivity, "Показывать, кто написал сообщение", 13f, R.color.textSecondary),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(2) })
        row.addView(mid, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(Ui.switch(this@ChatSettingsActivity, LocalPrefs.chatShowNames) { on ->
            LocalPrefs.chatShowNames = on
        }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        card.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        return card
    }

    private fun refreshSwatches() {
        swatches.forEachIndexed { i, sw ->
            sw.background = GradientDrawable().apply {
                setColor(LocalPrefs.chatBgColors[i])
                shape = GradientDrawable.OVAL
                if (i == LocalPrefs.chatBgIndex) setStroke(dp(3), color(R.color.accentText))
            }
        }
    }

    private fun refreshPatternChips() {
        patternChips.forEachIndexed { i, chip ->
            val selected = i == LocalPrefs.wallpaperPattern
            chip.textSize = 13f
            chip.setPadding(dp(16), dp(8), dp(16), dp(8))
            chip.setTextColor(color(if (selected) R.color.textPrimary else R.color.textSecondary))
            chip.paint.isFakeBoldText = selected
            chip.background = Ui.pill(this, if (selected) R.color.accent else R.color.bgTertiary)
        }
    }

    private fun applyPreview() {
        val r = dp(LocalPrefs.chatRadius).toFloat()
        val t = dp(4).toFloat()
        previewBubble.textSize = LocalPrefs.chatTextSize.toFloat()
        previewBubble.background = GradientDrawable().apply {
            setColor(color(R.color.messageOwn))
            cornerRadii = floatArrayOf(r, r, r, r, t, t, r, r)
        }
        val bg = LocalPrefs.chatBgColors[LocalPrefs.chatBgIndex]
        val wp = Wallpaper.drawable(this, LocalPrefs.wallpaperPattern, bg)
        if (wp != null) previewWrap.background = wp else previewWrap.setBackgroundColor(bg)
    }
}