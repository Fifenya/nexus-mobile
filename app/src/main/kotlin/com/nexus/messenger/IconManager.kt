package com.nexus.messenger

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconManager {
    enum class IconStyle(val aliasName: String, val displayName: String, val drawable: Int) {
        DEFAULT("MainActivityDefault", "Стандартная", R.drawable.icon_default),
        NEON("MainActivityNeon", "Неоновая", R.drawable.icon_neon),
        GOLD("MainActivityGold", "Золотая", R.drawable.icon_gold),
        MONO("MainActivityMono", "Минималистичная", R.drawable.icon_mono);
    }

    private const val PREF_KEY = "active_icon"

    fun getCurrent(context: Context): IconStyle {
        val prefs = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString(PREF_KEY, IconStyle.DEFAULT.aliasName)
        return IconStyle.values().find { it.aliasName == name } ?: IconStyle.DEFAULT
    }

    fun setCurrent(context: Context, style: IconStyle) {
        val prefs = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_KEY, style.aliasName).apply()

        val pm = context.packageManager
        // Включаем выбранный alias
        pm.setComponentEnabledSetting(
            ComponentName(context, "${context.packageName}.${style.aliasName}"),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        // Отключаем остальные
        IconStyle.values().filter { it != style }.forEach {
            pm.setComponentEnabledSetting(
                ComponentName(context, "${context.packageName}.${it.aliasName}"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
