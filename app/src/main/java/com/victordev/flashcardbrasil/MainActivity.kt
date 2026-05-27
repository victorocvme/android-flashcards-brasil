package com.victordev.flashcardbrasil

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.room.Room
import com.victordev.flashcardbrasil.core.bridges.CardsBridge
import com.victordev.flashcardbrasil.core.bridges.EmptyBridge
import com.victordev.flashcardbrasil.core.bridges.DeckBridge
import com.victordev.flashcardbrasil.config.AppDatabase
import com.victordev.flashcardbrasil.core.service.CardService
import com.victordev.flashcardbrasil.core.service.DeckService
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.victordev.flashcardbrasil.core.AppInitializer
import com.victordev.flashcardbrasil.core.InitResult
import com.victordev.flashcardbrasil.core.bridges.GuestBridge
import com.victordev.flashcardbrasil.core.service.GuestService
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // handleImportIntent(intent)
        // Garante que o layout ocupe a tela toda corretamente

        setContent {
            var isLoading by remember { mutableStateOf(true) }
            var statusText by remember { mutableStateOf("Verificando atualizações...") }
            var initResult by remember { mutableStateOf<InitResult?>(null) }

            LaunchedEffect(Unit) {

                val result = AppInitializer.initialize(this@MainActivity) {
                    status -> statusText = status
                }

                initResult = result
                isLoading = false
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = statusText)
                    }
                }

                initResult is InitResult.NoInternetFirstDownload -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sem internet. A primeira conexão precisa de internet para baixar os recursos iniciais. Conecte-se e abra o app novamente.")
                    }
                }

                else -> {
                    val url = if (BuildConfig.DEBUG) {
                        "http://192.168.1.104:4200"
                    } else {
                        "file://${filesDir.absolutePath}/www/index.html"
                    }

                    WebViewScreen(url)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleImportIntent(intent)
    }

    private fun handleImportIntent(intent: Intent?) {
        val uri = intent?.data ?: return

        if (uri.scheme != "flashcardsbrasil" || uri.host != "import") {
            return
        }

        val encodedData = uri.getQueryParameter("data")
        if (encodedData.isNullOrBlank()) {
            Log.w(TAG, "Deep link de importacao recebido sem parametro data: $uri")
            return
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
        }.onFailure { error ->
            Log.e(TAG, "Falha ao processar deep link de importacao: $uri", error)
        }
    }

    private fun decodeBase64(data: String): ByteArray {
        return runCatching {
            Base64.decode(data, Base64.DEFAULT)
        }.getOrElse {
            Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP)
        }
    }

    companion object {
        private const val TAG = "FlashcardImport"
        private const val LOG_CHUNK_SIZE = 3_000
    }

}

@Composable
fun WebViewScreen(url: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {

                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                val webSettings = settings

                webSettings.javaScriptEnabled = true
                webSettings.domStorageEnabled = true
                webSettings.loadWithOverviewMode = true
                webSettings.useWideViewPort = true
                webSettings.allowFileAccess = true
                webSettings.allowContentAccess = true
                webSettings.allowFileAccessFromFileURLs = true
                webSettings.allowUniversalAccessFromFileURLs = true
                // 🔥 ESSENCIAL PRA DEV
                webSettings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE

                // 🔥 Limpa qualquer cache antigo
                clearCache(true)
                clearHistory()

                // 🔥 Evita cache agressivo (hack eficiente)
                val finalUrl = "$url"

                val db = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "flashcards-db"
                ).fallbackToDestructiveMigration(true).build()

                val service = DeckService(db)
                val cardService = CardService(db)
                addJavascriptInterface(DeckBridge(service),"FlashcardApi")
                addJavascriptInterface(CardsBridge(cardService), "CardApi")
                addJavascriptInterface(EmptyBridge(), "Android")
                addJavascriptInterface(GuestBridge(GuestService(context)), "Guest")
                loadUrl(finalUrl)
            }
        }
    )
}

fun isInternetAvailable(): Boolean {
    return try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://www.google.com.br")
            .build()
        val response = client.newCall(request).execute()
        response.isSuccessful
    } catch (e: Exception) {
        false
    }
}
