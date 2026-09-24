package com.nexus.messenger

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconManager {
    enum class IconStyle(val aliasName: String, val displayName: String, val drawable: Int) {
        CLASSIC("MainActivityClassic", "Классическая", R.drawable.classic),
        NEON("MainActivityNeon", "Неоновая", R.drawable.neon),
        CRIMSON("MainActivityCrimson", "Багровая", R.drawable.crimson),
        LIGHT("MainActivityLight", "Светлая", R.drawable.light),
        EMBOSS("MainActivityEmboss", "Объёмная", R.drawable.emboss),
        LINE("MainActivityLine", "Линейная", R.drawable.line),
        BUBBLE("MainActivityBubble", "Пузырьковая", R.drawable.bubble);
    }

    private const val PREF_KEY = "active_icon"

    fun getCurrent(context: Context): IconStyle {
        val prefs = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString(PREF_KEY, IconStyle.CLASSIC.aliasName)
        return IconStyle.values().find { it.aliasName == name } ?: IconStyle.CLASSIC
    }

    fun setCurrent(context: Context, style: IconStyle) {
        val prefs = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY, style.aliasName).apply()

        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            ComponentName(context, "${context.packageName}.${style.aliasName}"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        IconStyle.values().filter { it != style }.forEach {
            pm.setComponentEnabledSetting(
                ComponentName(context, "${context.packageName}.${it.aliasName}"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
