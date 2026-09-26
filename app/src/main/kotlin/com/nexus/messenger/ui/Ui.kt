package com.nexus.messenger.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
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

    fun pillColor(ctx: Context, color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(999).toFloat()
    }

    fun tile(ctx: Context, color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(10).toFloat()
    }

    fun tileCircle(ctx: Context, color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        shape = GradientDrawable.OVAL
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

    /** Кастомный снекбар вместо системного Toast */
    fun snackbar(activity: Activity, text: String) {
        val content = activity.findViewById<FrameLayout>(android.R.id.content)
        val tv = TextView(activity).apply {
            this.text = text
            textSize = 14f
            setTextColor(activity.resources.getColor(R.color.textPrimary, null))
            background = pill(activity, R.color.bgTertiary)
            setPadding(dp(18), dp(12), dp(18), dp(12))
            elevation = dp(10).toFloat()
            maxLines = 3
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(96)
            leftMargin = dp(16)
            rightMargin = dp(16)
        }
        content.addView(tv, lp)
        tv.postDelayed({ content.removeView(tv) }, 2600)
    }
}

/** Кастомный диалог в стиле Nexus (вместо системных AlertDialog) */
class NxDialog(private val ctx: Context) {
    private val card = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.card(ctx)
        setPadding(dp(20), dp(18), dp(20), dp(10))
    }
    private val buttons = mutableListOf<Pair<String, () -> Unit>>()
    private var itemsView: LinearLayout? = null
    private var dialog: Dialog? = null

    fun title(s: String): NxDialog {
        card.addView(Ui.text(ctx, s, 18f, R.color.textPrimary, true),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) })
        return this
    }

    fun message(s: String): NxDialog {
        card.addView(Ui.text(ctx, s, 14f, R.color.textSecondary),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) })
        return this
    }

    fun view(v: View): NxDialog {
        card.addView(v, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })
        return this
    }

    fun items(list: List<String>, onPick: (Int) -> Unit): NxDialog {
        val wrap = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        list.forEachIndexed { i, label ->
            val row = Ui.text(ctx, label, 16f, R.color.textPrimary)
            row.setPadding(dp(4), dp(14), dp(4), dp(14))
            row.setOnClickListener {
                dialog?.dismiss()
                onPick(i)
            }
            wrap.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            if (i < list.size - 1) {
                wrap.addView(View(ctx).apply {
                    setBackgroundColor(ctx.resources.getColor(R.color.divider, null))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1))
            }
        }
        itemsView = wrap
        card.addView(wrap, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
        return this
    }

    fun button(label: String, cb: () -> Unit): NxDialog {
        buttons.add(label to cb)
        return this
    }

    fun show(): Dialog {
        if (buttons.isNotEmpty()) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
            }
            buttons.forEach { (label, cb) ->
                val b = Ui.text(ctx, label, 15f, R.color.accentText, true)
                b.setPadding(dp(14), dp(12), dp(14), dp(12))
                b.setOnClickListener {
                    dialog?.dismiss()
                    cb()
                }
                row.addView(b)
            }
            card.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) })
        }
        val d = Dialog(ctx)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(card)
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        d.setCanceledOnTouchOutside(true)
        d.show()
        d.window?.setLayout(
            (ctx.resources.displayMetrics.widthPixels * 0.86).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dialog = d
        return d
    }
}