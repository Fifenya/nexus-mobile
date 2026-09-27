package com.nexus.messenger

import android.app.Application
import com.nexus.messenger.data.LocalPrefs
import com.nexus.messenger.data.Theme

class NexusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LocalPrefs.init(this)
        Theme.load(this)
    }
}