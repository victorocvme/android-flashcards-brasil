package com.victordev.flashcardbrasil.core.bridges

import android.webkit.JavascriptInterface
import com.victordev.flashcardbrasil.core.service.GuestService

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

    }

    @JavascriptInterface
    fun isGuest() {

    }

}