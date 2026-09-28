package com.nexus.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.Theme

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // LocalPrefs инициализируется автоматически в NexusApp.onCreate()
        // Theme.load теперь безопасно принимает Context
        Theme.load(this)

        if (Store.token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            startActivity(Intent(this, ChatsActivity::class.java))
        }
        finish()
    }
}