
package com.victordev.flashcardbrasil
import android.content.Context
import android.widget.Toast
import android.webkit.JavascriptInterface

public class WebAppInterface(private val context: Context) {

    @JavascriptInterface
    fun saveCard(cardJson : String) {

    }

    @JavascriptInterface
    fun getCards() : String {
        return """[{"id" : 1}]"""
    }
}
