package com.nexus.messenger.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object Store {
    private const val NAME = "nexus_store"
    private lateinit var prefs: SharedPreferences

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    var token: String?
        get() = prefs.getString("token", null)
        set(v) = prefs.edit().apply {
            if (v == null) remove("token") else putString("token", v)
        }.apply()

    var user: User?
        get() = prefs.getString("user", null)?.let {
            try { User.fromJson(JSONObject(it)) } catch (_: Exception) { null }
        }
        set(v) = prefs.edit().apply {
            if (v == null) remove("user")
            else putString("user", JSONObject().apply {
                put("id", v.id)
                put("username", v.username)
                v.displayName?.let { put("displayName", it) }
                v.bio?.let { put("bio", it) }
            }.toString())
        }.apply()

    var apiBase: String
        get() = prefs.getString("api_base", "http://192.168.1.103:5173")!!
        set(v) {
            if (v.contains("github.io")) return
            prefs.edit().putString("api_base", v.trim()).apply()
        }

    /** Тест-режим: локальная имитация сервера без сети */
    var testMode: Boolean
        get() = prefs.getBoolean("test_mode", false)
        set(v) = prefs.edit().putBoolean("test_mode", v).apply()

    fun logout() {
        prefs.edit().clear().apply()
    }
}