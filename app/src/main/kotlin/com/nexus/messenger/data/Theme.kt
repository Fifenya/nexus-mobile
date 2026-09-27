package com.nexus.messenger.data

import android.content.Context
import android.graphics.Color
import com.nexus.messenger.NexusApp
import com.nexus.messenger.R
import org.json.JSONObject

object Theme {
    private var colors: JSONObject? = null

    fun load(@Suppress("UNUSED_PARAMETER") ctx: Context) {
        colors = LocalPrefs.themeJson?.let {
            try { JSONObject(it) } catch (e: Exception) { null }
        }
    }

    fun reset(@Suppress("UNUSED_PARAMETER") ctx: Context) {
        LocalPrefs.themeJson = null
        colors = null
    }

    val isActive: Boolean get() = colors != null

    fun opt(key: String): Int? {
        val s = colors?.optString(key) ?: return null
        return if (s.startsWith("#")) {
            try { Color.parseColor(s) } catch (e: Exception) { null }
        } else null
    }

    fun lighten(c: Int, f: Float): Int = mix(c, Color.WHITE, f)
    fun darken(c: Int, f: Float): Int = mix(c, Color.BLACK, f)

    private fun mix(a: Int, b: Int, f: Float): Int {
        val ar = (a shr 16) and 0xFF; val ag = (a shr 8) and 0xFF; val ab = a and 0xFF
        val br = (b shr 16) and 0xFF; val bg = (b shr 8) and 0xFF; val bb = b and 0xFF
        val r = (ar + (br - ar) * f).toInt()
        val g = (ag + (bg - ag) * f).toInt()
        val bl = (ab + (bb - ab) * f).toInt()
        return Color.argb((a ushr 24), r, g, bl)
    }

    private fun withAlpha(c: Int, alpha: Int): Int = (alpha shl 24) or (c and 0x00FFFFFF)

    fun surface2(): Int? = opt("surface2") ?: opt("surface")?.let { lighten(it, 0.10f) }
    fun accentAlpha(alpha: Int): Int? = opt("accent")?.let { withAlpha(it, alpha) }

    fun bgGradient(): IntArray? {
        val bg = opt("background") ?: return null
        return intArrayOf(lighten(bg, 0.10f), bg, darken(bg, 0.45f))
    }

    fun color(@Suppress("UNUSED_PARAMETER") ctx: Context, res: Int): Int {
        val c = when (res) {
            R.color.bgPrimary -> opt("background")
            R.color.bgSecondary -> opt("surface")
            R.color.bgTertiary, R.color.bgInput, R.color.border, R.color.divider -> surface2()
            R.color.accent, R.color.accentPressed, R.color.messageOwn, R.color.badge -> opt("accent")
            R.color.accentText -> opt("accent")?.let { lighten(it, 0.30f) }
            R.color.textPrimary -> opt("text")
            R.color.textSecondary -> opt("textSecondary") ?: opt("text")?.let { darken(it, 0.35f) }
            R.color.textMuted -> opt("textSecondary")?.let { darken(it, 0.25f) } ?: opt("text")?.let { darken(it, 0.55f) }
            R.color.messageOther -> opt("surface")
            else -> null
        }
        return c ?: NexusApp.context().resources.getColor(res, null)
    }
}