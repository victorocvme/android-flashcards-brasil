package com.victordev.flashcardbrasil.core.bridges

import android.webkit.JavascriptInterface
import com.victordev.flashcardbrasil.core.service.DeckService
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import com.google.gson.Gson;

class DeckBridge(
    private val service: DeckService
) {

    private val gson = Gson()

    @JavascriptInterface
    fun getDecks(): String {
        return try {
            val decks = runBlocking {
                service.getAllDecks()
            }

            gson.toJson(decks)

        } catch (e: Exception) {
            e.printStackTrace()
            "{\"error\":\"${e.message}\"}"
        }
    }

    @JavascriptInterface
    fun createDeck(json: String): String = runBlocking {
        try {
            val obj = JSONObject(json)
            val titulo = obj.getString("titulo")
            val metodo = "SM2"

            // A última linha do bloco é o que será retornado pelo runBlocking
            service.createDeck(titulo, metodo)
        } catch (e: Exception) {
            e.printStackTrace()
            // Retorna o JSON de erro em caso de falha
            "{\"error\":\"${e.message}\"}"
        }
    }

}