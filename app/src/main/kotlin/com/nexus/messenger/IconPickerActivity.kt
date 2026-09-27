package com.nexus.messenger

import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.nexus.messenger.ui.Ui
import com.nexus.messenger.ui.dp

data class IconOption(val drawable: Int, val alias: String, val name: String)

object IconManager {
    val options = listOf(
        IconOption(R.drawable.classic, "com.nexus.messenger.MainActivityClassic", "Классическая"),
        IconOption(R.drawable.neon, "com.nexus.messenger.MainActivityNeon", "Неон"),
        IconOption(R.drawable.crimson, "com.nexus.messenger.MainActivityCrimson", "Багровая"),
        IconOption(R.drawable.light, "com.nexus.messenger.MainActivityLight", "Светлая"),
        IconOption(R.drawable.emboss, "com.nexus.messenger.MainActivityEmboss", "Тиснение"),
        IconOption(R.drawable.line, "com.nexus.messenger.MainActivityLine", "Контурная"),
        IconOption(R.drawable.bubble, "com.nexus.messenger.MainActivityBubble", "Пузырь")
    )

    fun getCurrent(activity: Activity): IconOption {
        val pm = activity.packageManager
        val pkg = activity.packageName
        for (opt in options) {
            val state = pm.getComponentEnabledSetting(ComponentName(pkg, opt.alias))
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && opt.alias.endsWith("Classic")
            ) return opt
        }
        return options.first()
    }

    fun apply(activity: Activity, option: IconOption) {
        val pm = activity.packageManager
        val pkg = activity.packageName
        for (opt in options) {
            val state = if (opt.alias == option.alias)
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            pm.setComponentEnabledSetting(
                ComponentName(pkg, opt.alias),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

class IconPickerActivity : Activity() {

    private lateinit var currentLabel: TextView
    private lateinit var preview: ImageView

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
        back.setOnClickListener { finish()