package com.nexus.messenger.data

import android.content.Context
import java.util.Locale

object LocalPrefs {
    private lateinit var prefs: android.content.SharedPreferences

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("nexus_local", Context.MODE_PRIVATE)
    }

    val chatBgColors = intArrayOf(
        0xFF150809.toInt(), 0xFF1C0E11.toInt(), 0xFF0C0C0C.toInt(),
        0xFF141018.toInt(), 0xFF101418.toInt()
    )

    var chatTextSize: Int
        get() = prefs.getInt("chat_text_size", 15)
        set(v) = prefs.edit().putInt("chat_text_size", v).apply()

    var chatRadius: Int
        get() = prefs.getInt("chat_radius", 16)
        set(v) = prefs.edit().putInt("chat_radius", v).apply()

    var chatBgIndex: Int
        get() = prefs.getInt("chat_bg_index", 0)
        set(v) = prefs.edit().putInt("chat_bg_index", v).apply()

    var wallpaperPattern: Int
        get() = prefs.getInt("wallpaper_pattern", 0)
        set(v) = prefs.edit().putInt("wallpaper_pattern", v).apply()

    var chatShowNames: Boolean
        get() = prefs.getBoolean("chat_show_names", true)
        set(v) = prefs.edit().putBoolean("chat_show_names", v).apply()

    var notifEnabled: Boolean
        get() = prefs.getBoolean("notif_enabled", true)
        set(v) = prefs.edit().putBoolean("notif_enabled", v).apply()

    var notifSound: Boolean
        get() = prefs.getBoolean("notif_sound", true)
        set(v) = prefs.edit().putBoolean("notif_sound", v).apply()

    var notifVibro: Boolean
        get() = prefs.getBoolean("notif_vibro", true)
        set(v) = prefs.edit().putBoolean("notif_vibro", v).apply()

    var notifPreview: Boolean
        get() = prefs.getBoolean("notif_preview", true)
        set(v) = prefs.edit().putBoolean("notif_preview", v).apply()

    var forceRu: Boolean
        get() = prefs.getBoolean("force_ru", false)
        set(v) = prefs.edit().putBoolean("force_ru", v).apply()

    var privacyOnline: String
        get() = prefs.getString("privacy_online", "Все")!!
        set(v) = prefs.edit().putString("privacy_online", v).apply()

    var privacyPhoto: String
        get() = prefs.getString("privacy_photo", "Все")!!
        set(v) = prefs.edit().putString("privacy_photo", v).apply()

    var privacyBio: String
        get() = prefs.getString("privacy_bio", "Все")!!
        set(v) = prefs.edit().putString("privacy_bio", v).apply()

    var powerSave: Boolean
        get() = prefs.getBoolean("power_save", false)
        set(v) = prefs.edit().putBoolean("power_save", v).apply()

    var themeJson: String?
        get() = prefs.getString("theme_json", null)
        set(v) = prefs.edit().apply {
            if (v == null) remove("theme_json") else putString("theme_json", v)
        }.apply()

    var themeId: String?
        get() = prefs.getString("theme_id", null)
        set(v) = prefs.edit().apply {
            if (v == null) remove("theme_id") else putString("theme_id", v)
        }.apply()

    fun formatLocale(): Locale =
        if (forceRu) Locale("ru") else Locale.getDefault()

    fun isMuted(@Suppress("UNUSED_PARAMETER") ctx: Context, chatId: String): Boolean =
        prefs.getStringSet("muted_chats", emptySet())?.contains(chatId) == true

    fun setMuted(@Suppress("UNUSED_PARAMETER") ctx: Context, chatId: String, muted: Boolean) {
        val s = prefs.getStringSet("muted_chats", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (muted) s.add(chatId) else s.remove(chatId)
        prefs.edit().putStringSet("muted_chats", s).apply()
    }

    fun presenceIntervalMs(): Long = if (powerSave) 60000L else 20000L

    fun cacheSize(@Suppress("UNUSED_PARAMETER") ctx: Context): Long {
        val c = ctx.getSharedPreferences("nexus_cache", Context.MODE_PRIVATE)
        var sum = 0L
        c.all.values.forEach { sum += ((it as? String)?.length ?: 0).toLong() }
        return sum * 2
    }

    fun clearCache(@Suppress("UNUSED_PARAMETER") ctx: Context) {
        ctx.getSharedPreferences("nexus_cache", Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun humanSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes Б"
        bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f КБ", bytes / 1024.0)
        else -> String.format(Locale.US, "%.2f МБ", bytes / (1024.0 * 1024.0))
    }
}