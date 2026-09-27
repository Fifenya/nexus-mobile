package com.nexus.messenger

import android.app.Application
import android.content.Context

class NexusApp : Application() {
    companion object {
        lateinit var instance: NexusApp
            private set

        fun context(): Context = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}