package com.nexus.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.nexus.messenger.data.Api
import com.nexus.messenger.data.LocalPrefs
import com.nexus.messenger.data.Store
import com.nexus.messenger.data.Theme

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        LocalPrefs.init(this)
        Theme.load(this)
        Api.resolveServer {
            runOnUiThread {
                val target = if (Store.token != null) ChatsActivity::class.java else LoginActivity::class.java
                startActivity(Intent(this, target))
                finish()
            }
        }
    }
}