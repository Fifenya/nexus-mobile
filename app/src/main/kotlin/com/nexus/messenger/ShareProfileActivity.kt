package com.nexus.messenger

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.nexus.messenger.data.Store
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp

class ShareProfileActivity : Activity() {
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
        header.addView(Ui.text(this, "Мой QR-код", 20f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
        root.addView(header, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val username = Store.user?.username ?: "—"
        val link = "nexus://user/@$username"

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(24).toFloat()
            }
            setPadding(dp(24), dp(28), dp(24), dp(24))
        }

        val qrBitmap = generateQr(link, dp(240))
        if (qrBitmap != null) {
            card.addView(ImageView(this).apply {
                setImageBitmap(qrBitmap)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LinearLayout.LayoutParams(dp(240), dp(240)))
        } else {
            card.addView(Ui.text(this, "QR недоступен", 16f, R.color.textMuted),
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }

        val nameTv = TextView(this).apply {
            text = "@$username"
            textSize = 18f
            paint.isFakeBoldText = true
            setTextColor(0xFF150809.toInt())
            gravity = Gravity.CENTER
        }
        card.addView(nameTv, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(16) })

        val dispTv = TextView(this).apply {
            text = Store.user?.displayName ?: username
            textSize = 13f
            setTextColor(0xFF6B5456.toInt())
            gravity = Gravity.CENTER
        }
        card.addView(dispTv, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = dp(2) })

        root.addView(card, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            leftMargin = dp(24); rightMargin = dp(24); topMargin = dp(28)
        })

        val linkTv = Ui.text(this, link, 12f, R.color.textMuted)
        linkTv.gravity = Gravity.CENTER
        root.addView(linkTv, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(14) })

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(18), dp(16), 0)
        }
        val copyBtn = actionBtn("Скопировать") {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("nexus", "@$username\n$link"))
            Ui.snackbar(this, "Скопировано ✓")
        }
        btnRow.addView(copyBtn, LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(6) })
        val shareBtn = actionBtn("Поделиться") {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Мой профиль в Nexus: @$username\n$link")
            }
            startActivity(Intent.createChooser(send, "Поделиться профилем"))
        }
        btnRow.addView(shareBtn, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(6) })
        root.addView(btnRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        val note = Ui.text(this, "Друг может отсканировать код камерой или ввести ссылку вручную в поиске контактов.", 12f, R.color.textMuted)
        root.addView(note, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            leftMargin = dp(24); rightMargin = dp(24); topMargin = dp(18)
        })

        setContentView(root)
    }

    private fun color(res: Int): Int = resources.getColor(res, null)

    private fun actionBtn(label: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            paint.isFakeBoldText = true
            background = Ui.pill(this@ShareProfileActivity, R.color.accent)
            elevation = dp(4).toFloat()
            setOnClickListener { onClick() }
        }

    private fun generateQr(content: String, sizePx: Int): Bitmap? {
        return try {
            val hints = mapOf<EncodeHintType, Any>(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val fg = 0xFFDC2626.toInt()
            val bg = 0xFFFFFFFF.toInt()
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) fg else bg)
                }
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }
}