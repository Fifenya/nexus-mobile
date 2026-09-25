package com.nexus.messenger.ui

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.nexus.messenger.ChatsActivity
import com.nexus.messenger.R
import com.nexus.messenger.SettingsActivity
import com.nexus.messenger.ProfileActivity

object BottomNav {
    fun attach(frame: FrameLayout, activity: Activity, current: String) {
        val ctx = activity
        val bar = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = Ui.pill(ctx, R.color.bgSecondary)
            elevation = dp(8).toFloat()
            setPadding(dp(6), dp(6), dp(6), dp(6))
            gravity = Gravity.CENTER_VERTICAL
        }
        tab(bar, activity, "chats", "Чаты", R.drawable.ic_chat, current)
        tab(bar, activity, "settings", "Настройки", R.drawable.ic_gear, current)
        tab(bar, activity, "profile", "Профиль", R.drawable.ic_person, current)

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            leftMargin = dp(12); rightMargin = dp(12); bottomMargin = dp(10)
        }
        frame.addView(bar, lp)
    }

    private fun tab(bar: LinearLayout, activity: Activity, id: String, label: String, iconRes: Int, current: String) {
        val ctx = activity
        val active = id == current
        val v = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(8), dp(4), dp(8))
            if (active) background = Ui.pill(ctx, R.color.bgTertiary)
        }
        val iv = ImageView(ctx).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(
                ctx.resources.getColor(if (active) R.color.accentText else R.color.textSecondary, null)
            )
        }
        v.addView(iv, LinearLayout.LayoutParams(dp(22), dp(22)))
        val tv = TextView(ctx).apply {
            text = label
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(ctx.resources.getColor(if (active) R.color.accentText else R.color.textSecondary, null))
        }
        v.addView(tv, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(2) })
        v.setOnClickListener {
            if (!active) {
                val cls = when (id) {
                    "chats" -> ChatsActivity::class.java
                    "settings" -> SettingsActivity::class.java
                    else -> ProfileActivity::class.java
                }
                activity.startActivity(Intent(activity, cls))
                activity.finish()
                activity.overridePendingTransition(0, 0)
            }
        }
        bar.addView(v, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }
}
