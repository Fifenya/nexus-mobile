package com.nexus.messenger

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.ImageLoader
import com.nexus.messenger.data.Mote
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp
import org.json.JSONArray
import org.json.JSONObject

class MotesPickerActivity : Activity() {
    private lateinit var chatId: String
    private lateinit var grid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = intent.getStringExtra("chatId") ?: run { finish(); return }

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
        header.addView(Ui.text(this, "Отправить мот", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val scroll = ScrollView(this)
        grid = GridLayout(this).apply {
            columnCount = 3
            setPadding(dp(8), dp(8), dp(8), dp(24))
        }
        scroll.addView(grid, ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        setContentView(root)

        load()
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun load() {
        Api.get("/motes/gallery") { code, body ->
            runOnUiThread {
                grid.removeAllViews()
                if (code != 200) {
                    grid.addView(Ui.text(this, "Моты недоступны", 14f, R.color.textMuted),
                        GridLayout.LayoutParams().apply { width = MATCH_PARENT; setMargins(dp(16), dp(24), dp(16), dp(16)) })
                    return@runOnUiThread
                }
                val arr = Api.parseArray(body)
                if (arr.length() == 0) {
                    grid.addView(Ui.text(this, "Галерея пуста — добавьте моты в разделе «Моты»", 14f, R.color.textMuted),
                        GridLayout.LayoutParams().apply { width = MATCH_PARENT; setMargins(dp(16), dp(24), dp(16), dp(16)) })
                    return@runOnUiThread
                }
                for (i in 0 until arr.length()) {
                    val mote = Mote.fromJson(arr.getJSONObject(i))
                    val cell = FrameLayout(this)
                    val img = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                    ImageLoader.load(this, mote.url, img)
                    cell.addView(img, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    cell.setOnClickListener { send(mote) }
                    val size = (resources.displayMetrics.widthPixels - dp(16)) / 3
                    grid.addView(cell, GridLayout.LayoutParams().apply {
                        width = size
                        height = size
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    })
                }
            }
        }
    }

    private fun send(mote: Mote) {
        val body = JSONObject()
            .put("text", "")
            .put("attachments", JSONArray().put(JSONObject()
                .put("type", "image")
                .put("url", mote.url)))
        Api.post("/chats/$chatId/messages", body) { code, resp ->
            runOnUiThread {
                if (code in 200..299) {
                    Ui.snackbar(this, "Мот отправлен ✓")
                    finish()
                } else {
                    Ui.snackbar(this, "Не отправлено: ${Api.friendlyError(resp)}")
                }
            }
        }
    }
}