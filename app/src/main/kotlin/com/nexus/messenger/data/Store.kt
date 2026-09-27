package com.nexus.messenger.data

object Store {
    var testMode: Boolean = false

    var token: String?
        get() = LocalPrefs.prefs?.getString("auth_token", null)
        set(value) = LocalPrefs.prefs?.edit()?.putString("auth_token", value)?.apply()

    var apiBase: String
        get() = LocalPrefs.getApiBase()
        set(value) = LocalPrefs.setApiBase(value)

    var user: User? = null

    fun logout() {
        token = null
        user = null
        testMode = false
    }
}