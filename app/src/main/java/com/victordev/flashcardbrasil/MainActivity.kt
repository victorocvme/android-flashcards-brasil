package com.victordev.flashcardbrasil

import android.content.Intent
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
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
import com.victordev.flashcardbrasil.core.importing.FlashcardPayloadImporter
import com.victordev.flashcardbrasil.core.importing.FlashcardQrImportHandler
import com.victordev.flashcardbrasil.core.importing.QrImportResult
import com.victordev.flashcardbrasil.core.service.GuestService
import com.victordev.flashcardbrasil.core.webview.WebViewNotifier
import com.victordev.flashcardbrasil.reminders.ReviewReminderInitializer
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val webViewNotifier = WebViewNotifier()

    private val importHandler by lazy {
        val db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "flashcards-db"
        ).fallbackToDestructiveMigration(true).build()

        FlashcardQrImportHandler(
            FlashcardPayloadImporter(
                deckService = DeckService(db),
                cardService = CardService(db)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReviewReminderInitializer.initialize(this)
        handleImportIntent(intent)
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
                        "http://192.168.1.106:4200"
                    } else {
                        "file://${filesDir.absolutePath}/www/index.html"
                    }

                    WebViewScreen(
                        url = url,
                        onWebViewCreated = ::onWebViewCreated,
                        onPageLoaded = ::onWebViewPageLoaded
                    )
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
        lifecycleScope.launch {
            val result = importHandler.handle(intent)
            when (result) {
                is QrImportResult.Success -> {
                    webViewNotifier.showWebNotification(
                        message = successMessageFor(result.importResult.type),
                        variant = "success"
                    )
                }

                is QrImportResult.Failure -> {
                    webViewNotifier.showWebNotification(
                        message = result.message,
                        variant = "error"
                    )
                }

                null -> Unit
            }
        }
    }

    private fun onWebViewCreated(webView: WebView) {
        webViewNotifier.attach(webView)
    }

    private fun onWebViewPageLoaded(webView: WebView) {
        webViewNotifier.onPageLoaded(webView)
    }

    private fun successMessageFor(importType: String): String {
        return when (importType) {
            DECKS_IMPORT_TYPE -> "Sucesso ao importar decks"
            CARDS_IMPORT_TYPE -> "Sucesso ao importar cards"
            else -> "Importacao concluida"
        }
    }

    companion object {
        private const val DECKS_IMPORT_TYPE = "decks"
        private const val CARDS_IMPORT_TYPE = "cards"
    }

}

@Composable
fun WebViewScreen(
    url: String,
    onWebViewCreated: (WebView) -> Unit = {},
    onPageLoaded: (WebView) -> Unit = {}
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                onWebViewCreated(this)

                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        onPageLoaded(view)
                    }
                }
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
