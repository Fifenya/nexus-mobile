package com.nexus.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.nexus.messenger.data.Store

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        val target = if (Store.token != null) ChatsActivity::class.java else LoginActivity::class.java
        startActivity(Intent(this, target))
        finish()
    }
}