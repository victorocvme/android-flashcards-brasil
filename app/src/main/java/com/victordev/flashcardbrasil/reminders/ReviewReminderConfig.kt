package com.victordev.flashcardbrasil.reminders

import com.victordev.flashcardbrasil.BuildConfig

object ReviewReminderConfig {
    const val WORK_NAME = "review_reminder_work"
    const val TEST_WORK_NAME = "review_reminder_test_work"
    const val CHANNEL_ID = "review_reminders"
    const val NOTIFICATION_ID = 20260101

    val repeatMinutes: Long = BuildConfig.REVIEW_REMINDER_REPEAT_MINUTES
    val isTestMode: Boolean = BuildConfig.REVIEW_REMINDERS_TEST_MODE
}
