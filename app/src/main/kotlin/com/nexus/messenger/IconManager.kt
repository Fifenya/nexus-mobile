package com.nexus.messenger

import android.app.Activity
import android.content.ComponentName
import android.content.pm.PackageManager

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

    fun setIcon(activity: Activity, option: IconOption) {
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