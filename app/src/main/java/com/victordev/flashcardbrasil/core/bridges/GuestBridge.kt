package com.victordev.flashcardbrasil.core.bridges

import android.webkit.JavascriptInterface
import com.victordev.flashcardbrasil.core.service.GuestService
import kotlinx.coroutines.runBlocking

class GuestBridge(
    private val guestService: GuestService
) {

    @JavascriptInterface
    fun saveGuest() {
        android.util.Log.d("GuestBridge","saveguest")
        guestService.saveGuest()
    }

    @JavascriptInterface
    fun clearGuest() {
        guestService.clearGuest()
    }

    @JavascriptInterface
    fun isGuest(): Boolean {
        return runBlocking {
            android.util.Log.d("GuestBridge","saveguest")
            guestService.isGuest()
        }
    }

}