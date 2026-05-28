package com.victordev.flashcardbrasil.core.importing

import android.content.Intent
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

class FlashcardQrImportHandler(
    private val importer: FlashcardPayloadImporter
) {

    suspend fun handle(intent: Intent?): QrImportResult? = withContext(Dispatchers.IO) {
        val uri = intent?.data ?: return@withContext null

        if (uri.scheme != SCHEME || uri.host != HOST) {
            return@withContext null
        }

        val encodedData = uri.getQueryParameter("data")
        if (encodedData.isNullOrBlank()) {
            Log.w(TAG, "Deep link de importacao recebido sem parametro data: $uri")
            return@withContext QrImportResult.Failure("Dados de importacao ausentes")
        }

        runCatching {
            val compressedBytes = decodeBase64(encodedData)
            val importedData = GZIPInputStream(ByteArrayInputStream(compressedBytes)).use {
                it.readBytes().toString(Charsets.UTF_8)
            }

            Log.d(TAG, "Dados importados recebidos (${importedData.length} caracteres):")
            importedData.chunked(LOG_CHUNK_SIZE).forEach { chunk ->
                Log.d(TAG, chunk)
            }

            val result = importer.importPayload(importedData)
            Log.d(TAG, "Importacao concluida: ${result.importedCount} item(ns) do tipo ${result.type}")
            QrImportResult.Success(result)
        }.getOrElse { error ->
            Log.e(TAG, "Falha ao processar deep link de importacao: $uri", error)
            QrImportResult.Failure(error.message ?: "Falha ao importar dados")
        }
    }

    private fun decodeBase64(data: String): ByteArray {
        val normalizedData = data.trim()
        val isBase64Url = normalizedData.any { it == '-' || it == '_' }

        val flags = if (isBase64Url) {
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        } else {
            Base64.DEFAULT
        }

        val valueToDecode = if (isBase64Url) {
            normalizedData
        } else {
            normalizedData.replace(' ', '+')
        }.withBase64Padding()

        return Base64.decode(valueToDecode, flags)
    }

    private fun String.withBase64Padding(): String {
        val missingPadding = (4 - length % 4) % 4
        return this + "=".repeat(missingPadding)
    }

    companion object {
        private const val SCHEME = "flashcardsbrasil"
        private const val HOST = "import"
        private const val TAG = "FlashcardImport"
        private const val LOG_CHUNK_SIZE = 3_000
    }
}

sealed class QrImportResult {
    data class Success(val importResult: ImportResult) : QrImportResult()
    data class Failure(val message: String) : QrImportResult()
}
