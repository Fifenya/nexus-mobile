package com.nexus.messenger.ui

import android.content.res.Resources

fun dp(value: Int): Int {
    return (value * Resources.getSystem().displayMetrics.density).toInt()
}