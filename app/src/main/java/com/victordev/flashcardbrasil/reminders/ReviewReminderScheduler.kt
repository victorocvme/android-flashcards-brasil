package com.victordev.flashcardbrasil.reminders

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.victordev.flashcardbrasil.workers.ReviewReminderWorker
import java.util.concurrent.TimeUnit

object ReviewReminderScheduler {
    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)

        if (ReviewReminderConfig.isTestMode) {
            workManager.cancelUniqueWork(ReviewReminderConfig.WORK_NAME)
            Log.d(TAG, "Scheduling test review reminder every ${ReviewReminderConfig.repeatMinutes} minute(s)")
            scheduleOneTimeForTest(context, ExistingWorkPolicy.KEEP)
            return
        }

        workManager.cancelUniqueWork(ReviewReminderConfig.TEST_WORK_NAME)
        Log.d(TAG, "Scheduling periodic review reminder every ${ReviewReminderConfig.repeatMinutes} minute(s)")

        val request = PeriodicWorkRequestBuilder<ReviewReminderWorker>(
            ReviewReminderConfig.repeatMinutes,
            TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            ReviewReminderConfig.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleOneTimeForTest(
        context: Context,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
    ) {
        val request = OneTimeWorkRequestBuilder<ReviewReminderWorker>()
            .setInitialDelay(ReviewReminderConfig.repeatMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ReviewReminderConfig.TEST_WORK_NAME,
            policy,
            request
        )
    }

    private const val TAG = "ReviewReminderScheduler"
}
