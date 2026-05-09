package com.victordev.flashcardbrasil.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.victordev.flashcardbrasil.config.AssetManager
import com.victordev.flashcardbrasil.core.model.AppVersion
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class UpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val localVersion = prefs.getString("version", "v0.0.0")

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("url_here")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = response.body?.string() ?: ""
                val remoteVersion = Gson().fromJson(json, AppVersion::class.java).latest

                if (localVersion != remoteVersion) {
                    val success = AssetManager.downloadAndUnzip(
                        applicationContext,
                        "url_here",
                        remoteVersion
                    )

                    if (success) {
                        prefs.edit().putString("version", remoteVersion).apply()
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
