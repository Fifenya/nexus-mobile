package com.nexus.messenger.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.TextView
import com.nexus.messenger.data.Theme

object Backgrounds {
    fun base(ctx: Context): GradientDrawable {
        val grad = Theme.bgGradient()
        return if (grad != null) {
            GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, grad)
        } else {
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF24090D.toInt(), 0xFF150809.toInt(), 0xFF0B0304.toInt())
            )
        }
    }

    private fun glowColor(ctx: Context, alpha: Int): Int {
        val a = Theme.accentAlpha(alpha)
        return a ?: ((alpha shl 24) or 0xDC2626)
    }

    fun glowTop(ctx: Context): GradientDrawable = GradientDrawable().apply {
        gradientType = GradientDrawable.RADIAL_GRADIENT
        gradientRadius = dp(340).toFloat()
        colors = intArrayOf(
            glowColor(ctx, 0x44),
            glowColor(ctx, 0x1A),
            glowColor(ctx, 0x00)
        )
        setGradientCenter(0.5f, 0.16f)
    }

    fun glowBottom(ctx: Context): GradientDrawable = GradientDrawable().apply {
        gradientType = GradientDrawable.RADIAL_GRADIENT
        gradientRadius = dp(300).toFloat()
        colors = intArrayOf(
            glowColor(ctx, 0x2E),
            glowColor(ctx, 0x00)
        )
        setGradientCenter(0.88f, 0.96f)
    }

    fun attach(frame: FrameLayout) {
        val ctx = frame.context
        frame.addView(View(ctx).apply { background = base(ctx) },
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        frame.addView(View(ctx).apply { background = glowTop(ctx) },
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        frame.addView(View(ctx).apply { background = glowBottom(ctx) },
            FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        frame.addView(TextView(ctx).apply {
            text = "N"
            textSize = 260f
            setTextColor(0x12FFFFFF)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
        }, FrameLayout.LayoutParams(MATCH_PARENT, dp(420)).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = dp(-60)
        })
    }
}