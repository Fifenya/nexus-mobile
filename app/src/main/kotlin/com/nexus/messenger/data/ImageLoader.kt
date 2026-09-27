package com.nexus.messenger.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL

object ImageLoader {
    private val cache = LruCache<String, Bitmap>(64)

    fun load(@Suppress("UNUSED_PARAMETER") ctx: android.content.Context, url: String, into: ImageView, placeholderColor: Int = 0xFF201012.toInt()) {
        into.setBackgroundColor(placeholderColor)
        into.setImageDrawable(null)
        cache.get(url)?.let { into.setImageBitmap(it); return }

        if (url.startsWith("mock://")) {
            val bmp = mockBitmap(url)
            cache.put(url, bmp)
            into.setImageBitmap(bmp)
            return
        }

        val full = if (url.startsWith("http")) url else Store.apiBase + url
        Thread {
            try {
                val conn = URL(full).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                if (conn.responseCode == 200) {
                    val bmp = BitmapFactory.decodeStream(conn.inputStream)
                    if (bmp != null) {
                        cache.put(url, bmp)
                        into.post { into.setImageBitmap(bmp) }
                    }
                }
            } catch (_: Exception) {
            }
        }.start()
    }

    private fun mockBitmap(url: String): Bitmap {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val seed = url.hashCode()
        val colors = intArrayOf(
            0xFFDC2626.toInt(), 0xFF7B6FE0.toInt(), 0xFF3FC1C9.toInt(),
            0xFFE09A3F.toInt(), 0xFF4CD964.toInt(), 0xFFE05576.toInt()
        )
        c.drawColor(colors[Math.abs(seed) % colors.size])
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x55FFFFFF
            textSize = 140f
            textAlign = Paint.Align.CENTER
        }
        c.drawText("★", size / 2f, size / 2f + 48f, p)
        return bmp
    }
}