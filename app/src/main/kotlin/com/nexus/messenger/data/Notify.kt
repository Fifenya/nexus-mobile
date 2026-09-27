package com.nexus.messenger.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nexus.messenger.ChatActivity
import com.nexus.messenger.R

object Notify {
    private const val CHANNEL = "nexus_messages"
    private var lastId = 1000

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getChannel(CHANNEL) == null) {
                val ch = NotificationChannel(
                    CHANNEL,
                    "Сообщения Nexus",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Новые сообщения в чатах"
                }
                nm.createChannel(ch)
            }
        }
    }

    fun message(ctx: Context, title: String, body: String, chatId: String, chatTitle: String?) {
        if (!LocalPrefs.notifEnabled) return
        ensureChannel(ctx)
        val intent = Intent(ctx, ChatActivity::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("title", chatTitle ?: title)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(
            ctx, lastId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val shownBody = if (LocalPrefs.notifPreview) body else "Новое сообщение"
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_send)
            .setContentTitle(title)
            .setContentText(shownBody)
            .setStyle(Notification.BigTextStyle().bigText(shownBody))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(lastId++, n)
    }
}