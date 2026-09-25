package com.nexus.messenger.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import com.nexus.messenger.R

object Ui {
    /** Скруглённая карточка (фон секций настроек) */
    fun card(ctx: Context): GradientDrawable = GradientDrawable().apply {
        setColor(ctx.resources.getColor(R.color.bgSecondary, null))
        cornerRadius = dp(16).toFloat()
    }

    /** Пилюля (поиск, бейджи, FAB, нижняя навигация) */
    fun pill(ctx: Context, colorRes: Int): GradientDrawable = GradientDrawable().apply {
        setColor(ctx.resources.getColor(colorRes, null))
        cornerRadius = dp(999).toFloat()
    }

    /** Пилюля произвольным цветом (без ресурса) */
    fun pillColor(ctx: Context, color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(999).toFloat()
    }

    /** Скруглённый квадрат-плитка под иконку */
    fun tile(ctx: Context, color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(10).toFloat()
    }

    /** Круг (аватарки, бейдж камеры) */
    fun tileCircle(ctx: Context, color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        shape = GradientDrawable.OVAL
    }

    /** Просто иконка с тинтом */
    fun icon(ctx: Context, res: Int, tint: Int): ImageView = ImageView(ctx).apply {
        setImageResource(res)
        imageTintList = ColorStateList.valueOf(tint)
    }

    /** Иконка на цветной плитке (как в настройках Telegram) */
    fun tileIcon(ctx: Context, iconRes: Int, tileColor: Int): ImageView = ImageView(ctx).apply {
        setImageResource(iconRes)
        imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
        background = tile(ctx, tileColor)
        setPadding(dp(8), dp(8), dp(8), dp(8))
    }

    /** Текстовая view */
    fun text(ctx: Context, s: String, sizeSp: Float, colorRes: Int, bold: Boolean = false): TextView =
        TextView(ctx).apply {
            text = s
            textSize = sizeSp
            setTextColor(ctx.resources.getColor(colorRes, null))
            if (bold) paint.isFakeBoldText = true
        }

    /** Тумблер в стиле Telegram (красный когда включён) */
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
