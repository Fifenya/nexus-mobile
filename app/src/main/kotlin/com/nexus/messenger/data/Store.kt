package com.nexus.messenger.data

object Store {
    var token: String? = null
    var user: User? = null
    var testMode: Boolean = false

    var apiBase: String
        get() = LocalPrefs.getApiBase()
        set(value) = LocalPrefs.setApiBase(value)

    fun logout() {
        token = null
        user = null
        testMode = false
    }
}