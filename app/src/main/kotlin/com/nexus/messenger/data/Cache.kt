package com.nexus.messenger.data

import android.content.Context

/** Простой дисковый кэш: чаты и сообщения доступны офлайн */
object Cache {
    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("nexus_cache", Context.MODE_PRIVATE)

    fun put(ctx: Context, key: String, value: String) {
        prefs(ctx).edit().putString(key, value).apply()
    }

    fun get(ctx: Context, key: String): String? =
        prefs(ctx).getString(key, null)
}