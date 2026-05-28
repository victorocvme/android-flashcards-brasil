package com.victordev.flashcardbrasil.core.webview

import android.util.Log
import android.webkit.WebView
import org.json.JSONObject

class WebViewNotifier {

    private var webView: WebView? = null
    private var isPageLoaded = false
    private val pendingNotifications = mutableListOf<WebNotification>()

    fun attach(webView: WebView) {
        this.webView = webView
        isPageLoaded = false
    }

    fun onPageLoaded(webView: WebView) {
        this.webView = webView
        isPageLoaded = true
        flushPendingNotifications()
    }

    fun showWebNotification(
        message: String,
        variant: String = "success",
        durationMs: Int = 2500
    ) {
        val notification = WebNotification(
            message = message,
            variant = variant,
            durationMs = durationMs
        )

        val currentWebView = webView
        if (currentWebView == null || !isPageLoaded) {
            Log.d(TAG, "WebView ainda nao pronta. Notificacao enfileirada: $variant")
            pendingNotifications.add(notification)
            return
        }

        currentWebView.evaluateJavascript(notification.toJavaScript(), null)
    }

    private fun flushPendingNotifications() {
        if (pendingNotifications.isEmpty()) {
            return
        }

        val currentWebView = webView
        if (currentWebView == null || !isPageLoaded) {
            return
        }

        pendingNotifications.toList().forEach { notification ->
            currentWebView.evaluateJavascript(notification.toJavaScript(), null)
        }
        pendingNotifications.clear()
    }

    private fun WebNotification.toJavaScript(): String {
        val payload = JSONObject()
            .put("message", message)
            .put("variant", variant)
            .put("durationMs", durationMs)

        return "window.FlashcardsBrasil?.showNotification(${payload});"
    }

    private data class WebNotification(
        val message: String,
        val variant: String,
        val durationMs: Int
    )

    companion object {
        private const val TAG = "WebViewNotifier"
    }
}
