package com.victordev.flashcardbrasil.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object ReviewNotificationChannel {
    fun ensureExists(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(ReviewReminderConfig.CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            ReviewReminderConfig.CHANNEL_ID,
            "Revisoes de flashcards",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Lembretes para revisar cartoes pendentes"
        }

        manager.createNotificationChannel(channel)
    }

    fun exists(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true

        val manager = context.getSystemService(NotificationManager::class.java)
        return manager.getNotificationChannel(ReviewReminderConfig.CHANNEL_ID) != null
    }
}
