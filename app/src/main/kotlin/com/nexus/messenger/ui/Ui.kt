package com.nexus.messenger.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import com.nexus.messenger.R

object Ui {
    fun card(ctx: Context): GradientDrawable = GradientDrawable().apply {
        setColor(ctx.resources.getColor(R.color.bgSecondary, null))
        cornerRadius = dp(16).toFloat()
    }

    fun pill(ctx: Context, colorRes: Int): GradientDrawable = GradientDrawable().apply {
        setColor(ctx.resources.getColor(colorRes, null))
        cornerRadius = dp(999).toFloat()
    }

    fun tile(ctx: Context, color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(10).toFloat()
    }

    fun icon(ctx: Context, res: Int, tint: Int): ImageView = ImageView(ctx).apply {
        setImageResource(res)
        imageTintList = ColorStateList.valueOf(tint)
    }

    fun tileIcon(ctx: Context, iconRes: Int, tileColor: Int): ImageView = ImageView(ctx).apply {
        setImageResource(iconRes)
        imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
        background = tile(ctx, tileColor)
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    fun text(ctx: Context, s: String, sizeSp: Float, colorRes: Int, bold: Boolean = false): TextView =
        TextView(ctx).apply {
            text = s
            textSize = sizeSp
            setTextColor(ctx.resources.getColor(colorRes, null))
            if (bold) paint.isFakeBoldText = true
        }

    fun switch(ctx: Context, checked: Boolean, onChange: (Boolean) -> Unit): Switch = Switch(ctx).apply {
        isChecked = checked
        trackTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(
                ctx.resources.getColor(R.color.accent, null),
                ctx.resources.getColor(R.color.bgTertiary, null)
            )
        )
        thumbTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(0xFFFFFFFF.toInt(), ctx.resources.getColor(R.color.textMuted, null))
        )
        setOnCheckedChangeListener { _, c -> onChange(c) }
    }
}
