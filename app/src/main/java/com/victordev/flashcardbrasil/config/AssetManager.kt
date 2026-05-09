package com.victordev.flashcardbrasil.config

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object AssetManager {
    private val client = OkHttpClient()

    fun downloadAndUnzip(context: Context, url: String, version: String): Boolean {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) return false

        val zipFile = File(context.filesDir, "update.zip")
        response.body?.byteStream()?.use { input ->
            FileOutputStream(zipFile).use { output -> input.copyTo(output) }
        }

        // Descompactar na pasta 'www'
        val targetDir = File(context.filesDir, "www")
        if (!targetDir.exists()) targetDir.mkdirs()

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) newFile.mkdirs()
                else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        zipFile.delete() // Apaga o zip após extrair
        return true
    }
}