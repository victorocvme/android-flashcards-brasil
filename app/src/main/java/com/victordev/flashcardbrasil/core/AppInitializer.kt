package com.victordev.flashcardbrasil.core

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.victordev.flashcardbrasil.config.AssetManager
import com.victordev.flashcardbrasil.core.model.AppVersion
import com.victordev.flashcardbrasil.isInternetAvailable
import com.victordev.flashcardbrasil.workers.UpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object AppInitializer {

    private const val PREFS_NAME = "app_prefs"
    private const val VERSION_KEY = "version"

    private const val BASE_URL = "https://front-flashcards-brasil-zip.s3.sa-east-1.amazonaws.com"
    private const val VERSION_URL = "$BASE_URL/version.json"

    suspend fun initialize(
        context: Context,
        onStatusUpdate: (String) -> Unit
    ): InitResult {
        return withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val localVersion = prefs.getString(VERSION_KEY, null)

            prefs.edit()
                .remove(VERSION_KEY)
                .apply()

            if (localVersion == null) {

                if (!isInternetAvailable()) {
                    onStatusUpdate("Você precisa de internet para baixar os recursos iniciais.")
                    return@withContext InitResult.NoInternetFirstDownload
                }

                onStatusUpdate("Baixando recursos iniciais...")

                val remoteVersion = fetchRemoteVersion()
                    ?: return@withContext InitResult.NoInternetFirstDownload

                val success = AssetManager.downloadAndUnzip(
                    context,
                    "$BASE_URL/$remoteVersion/app.zip",
                    remoteVersion
                )

                if (success) {
                    prefs.edit().putString(VERSION_KEY, remoteVersion).apply()
                }
            }

            sheduleUpdateWorker(context)

            InitResult.Success
        }
    }

    private fun fetchRemoteVersion(): String? {
        return try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(VERSION_URL)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return null

            val json = response.body?.string() ?: return null

            Gson().fromJson(json, AppVersion::class.java).latest
        } catch (e: Exception) {
            null
        }
    }

    private fun sheduleUpdateWorker(context: Context) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1,
            TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "update_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

sealed class InitResult {
    object Success : InitResult()
    object NoInternetFirstDownload : InitResult()
}