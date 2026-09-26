package com.nexus.messenger.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.TextView

object Backgrounds {
    /** Вертикальный градиент тёмно-красного */
    fun base(ctx: Context): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(0xFF24090D.toInt(), 0xFF150809.toInt(), 0xFF0B0304.toInt())
    )

    /** Радиальное свечение акцента сверху по центру */
    fun glowTop(ctx: Context): GradientDrawable = GradientDrawable().apply {
        gradientType = GradientDrawable.RADIAL_GRADIENT
        gradientRadius = dp(340).toFloat()
        colors = intArrayOf(0x44DC2626, 0x1ADC2626, 0x00DC2626)
        centerX = 0.5f
        centerY = 0.16f
    }

    /** Тёплое свечение снизу справа */
    fun glowBottom(ctx: Context): GradientDrawable = GradientDrawable().apply {
        gradientType = GradientDrawable.RADIAL_GRADIENT
        gradientRadius = dp(300).toFloat()
        colors = intArrayOf(0x2E7F1D1D, 0x007F1D1D)
        centerX = 0.88f
        centerY = 0.96f
    }

    /** Навешивает слои на FrameLayout (вызывать до добавления контента) */
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