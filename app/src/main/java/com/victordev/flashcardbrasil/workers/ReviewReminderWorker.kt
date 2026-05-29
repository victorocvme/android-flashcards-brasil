package com.victordev.flashcardbrasil.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.victordev.flashcardbrasil.MainActivity
import com.victordev.flashcardbrasil.R
import com.victordev.flashcardbrasil.config.AppDatabase
import com.victordev.flashcardbrasil.reminders.ReviewNotificationChannel
import com.victordev.flashcardbrasil.reminders.ReviewNotificationMessageProvider
import com.victordev.flashcardbrasil.reminders.ReviewNotificationPermission
import com.victordev.flashcardbrasil.reminders.ReviewReminderConfig
import com.victordev.flashcardbrasil.reminders.ReviewReminderScheduler
import com.victordev.flashcardbrasil.utils.DateUtils
import kotlinx.coroutines.delay

class ReviewReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            ReviewNotificationChannel.ensureExists(applicationContext)

            val pendingCards = countPendingCards()
            Log.d(TAG, "Worker executed. pendingCards=$pendingCards testMode=${ReviewReminderConfig.isTestMode}")

            if (
                ReviewNotificationPermission.hasPermission(applicationContext) &&
                pendingCards > 0 &&
                ReviewNotificationChannel.exists(applicationContext)
            ) {
                showNotification(pendingCards)
                Log.d(TAG, "Notification shown")
            }

            if (ReviewReminderConfig.isTestMode) {
                ReviewReminderScheduler.scheduleOneTimeForTest(applicationContext)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun countPendingCards(): Int {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "flashcards-db"
        ).fallbackToDestructiveMigration(true).build()

        return try {
            db.cardDao().countCardsForReview(DateUtils.getDiasDesdeDataBase())
        } finally {
            db.close()
        }
    }

    private suspend fun showNotification(pendingCards: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = ReviewNotificationMessageProvider(applicationContext)
            .nextMessage(pendingCards)

        val notification = NotificationCompat.Builder(
            applicationContext,
            ReviewReminderConfig.CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(
                ContextCompat.getDrawable(applicationContext, R.mipmap.ic_launcher)?.toBitmap()
            )
            .setContentTitle(message.title)
            .setContentText(message.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.cancel(ReviewReminderConfig.NOTIFICATION_ID)
        delay(250)
        manager.notify(ReviewReminderConfig.NOTIFICATION_ID, notification)
    }

    private companion object {
        const val TAG = "ReviewReminderWorker"
    }
}
