package com.victordev.flashcardbrasil.reminders

import android.app.Activity

object ReviewReminderInitializer {
    fun initialize(activity: Activity) {
        ReviewNotificationChannel.ensureExists(activity)
        ReviewNotificationPermission.requestIfNeeded(activity)
        ReviewReminderScheduler.schedule(activity.applicationContext)
    }
}
