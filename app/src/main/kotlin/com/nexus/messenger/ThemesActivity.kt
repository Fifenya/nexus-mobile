package com.nexus.messenger

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.LocalPrefs
import com.nexus.messenger.data.Theme
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONObject

class ThemesActivity : Activity() {

    private lateinit var listCard: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bgPrimary))
        }
        root.addView(headerView("Темы оформления"), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(32))
        }

        val note = Ui.text(this, "Темы загружаются с сервера Nexus. Цвета применяются ко всему интерфейсу.", 12f, R.color.textMuted)
        content.addView(note, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(10) })

        listCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.card(this@ThemesActivity)
        }
        content.addView(listCard, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        scroll.addView(content, ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)

        load()
    }

    private fun color(res: Int): Int = Theme.color(this, res)

    private fun headerView(title: String): LinearLayout {
        val ctx = this
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(ctx.resources.getColor(R.color.bgSecondary, null))
            setPadding(dp(4), dp(10), dp(16), dp(10))
            val back = TextView(ctx).apply {
                text = "←"
                textSize = 24f
                setTextColor(ctx.resources.getColor(R.color.accentText, null))
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }
            back.setOnClickListener { finish() }
            addView(back)
            addView(Ui.text(ctx, title, 20f, R.color.textPrimary, true),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
        }
    }

    private fun load() {
        listCard.removeAllViews()
        val loading = Ui.text(this, "Загрузка тем…", 14f, R.color.textMuted)
        loading.setPadding(dp(16), dp(14), dp(16), dp(14))
        listCard.addView(loading, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        Api.get("/themes") { code, body ->
            runOnUiThread {
                listCard.removeAllViews()

                addThemeRow(
                    id = null,
                    name = "Стандартная (тёмно-красная)",
                    preview = intArrayOf(0xFF150809.toInt(), 0xFF201012.toInt(), 0xFFDC2626.toInt(), 0xFFFFFFFF.toInt()),
                    colorsJson = null
                )

                if (code == 200) {
                    val arr = if (body.trim().startsWith("[")) Api.parseArray(body)
                    else Api.parseObj(body)?.optJSONArray("items") ?: Api.parseArray("[]")
                    for (i in 0 until arr.length()) {
                        val t = arr.optJSONObject(i) ?: continue
                        val id = t.optString("id")
                        val name = t.optString("name").takeIf { it.isNotEmpty() && it != "null" } ?: "Тема $i"
                        val colors = t.optJSONObject("colors") ?: JSONObject()
                        val preview = intArrayOf(
                            parseOr(colors.optString("background"), 0xFF150809.toInt()),
                            parseOr(colors.optString("surface"), 0xFF201012.toInt()),
                            parseOr(colors.optString("accent"), 0xFFDC2626.toInt()),
                            parseOr(colors.optString("text"), 0xFFFFFFFF.toInt())
                        )
                        if (i > 0) {
                            listCard.addView(dividerView(), LinearLayout.LayoutParams(MATCH_PARENT, 1).apply { leftMargin = dp(16) })
                        }
                        addThemeRow(id, name, preview, colors)
                    }
                } else {
                    val err = Ui.text(this, "Сервер тем недоступен — показана только стандартная", 12f, R.color.textMuted)
                    err.setPadding(dp(16), dp(4), dp(16), dp(14))
                    listCard.addView(err, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                }
            }
        }
    }

    private fun dividerView() = View(this).apply { setBackgroundColor(color(R.color.divider)) }

    private fun parseOr(s: String?, fallback: Int): Int =
        if (s != null && s.startsWith("#")) {
            try { Color.parseColor(s) } catch (e: Exception) { fallback }
        } else fallback

    private fun addThemeRow(id: String?, name: String, preview: IntArray, colorsJson: JSONObject?) {
        val active = if (id == null) LocalPrefs.themeId == null else LocalPrefs.themeId == id
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val dots = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        preview.forEach { c ->
            dots.addView(View(this).apply {
                background = Ui.tileCircle(this@ThemesActivity, c)
            }, LinearLayout.LayoutParams(dp(18), dp(18)).apply { rightMargin = dp(4) })
        }
        row.addView(dots)

        row.addView(Ui.text(this, name, 16f, R.color.textPrimary),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(12) })

        if (active) {
            row.addView(Ui.icon(this, R.drawable.ic_check, color(R.color.accentText)),
                LinearLayout.LayoutParams(dp(22), dp(22)))
        }

        row.setOnClickListener {
            if (colorsJson == null) {
                Theme.reset(this)
            } else {
                LocalPrefs.themeJson = colorsJson.toString()
                LocalPrefs.themeId = id
                Theme.load(this)
            }
            Ui.snackbar(this, "Тема «$name» применена")
            recreate()
        }

        listCard.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }
}