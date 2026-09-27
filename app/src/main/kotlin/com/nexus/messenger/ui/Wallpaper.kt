package com.nexus.messenger.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

object Wallpaper {
    /** 0 — нет, 1 — точки, 2 — диагонали, 3 — круги */
    fun drawable(ctx: Context, pattern: Int, bgColor: Int): Drawable? {
        if (pattern <= 0) return null
        val size = dp(120)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(bgColor)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(16, 255, 255, 255)
            strokeCap = Paint.Cap.ROUND
        }
        when (pattern) {
            1 -> {
                p.strokeWidth = dp(3).toFloat()
                for (x in 0 until 4) {
                    for (y in 0 until 4) {
                        c.drawPoint(size / 4f * x + size / 8f, size / 4f * y + size / 8f, p)
                    }
                }
            }
            2 -> {
                p.strokeWidth = dp(2).toFloat()
                var i = -size
                while (i < size * 2) {
                    c.drawLine(i.toFloat(), 0f, (i + size).toFloat(), size.toFloat(), p)
                    i += dp(24)
                }
            }
            3 -> {
                p.strokeWidth = dp(2).toFloat()
                p.style = Paint.Style.STROKE
                c.drawCircle(size / 2f, size / 2f, size / 3f, p)
                c.drawCircle(size / 2f, size / 2f, size / 6f, p)
            }
        }
        return BitmapDrawable(ctx.resources, bmp).apply {
            tileModeX = Shader.TileMode.REPEAT
            tileModeY = Shader.TileMode.REPEAT
        }
    }
}