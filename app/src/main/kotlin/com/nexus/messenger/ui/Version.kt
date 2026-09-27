package com.nexus.messenger.ui

import android.content.Context

/** Читает versionName из app/build.gradle — единственное место, где меняется версия */
fun Context.appVersionName(): String = try {
    packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
} catch (e: Exception) {
    "1.0.0"
}