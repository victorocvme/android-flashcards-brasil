package com.victordev.flashcardbrasil.core.bridges

import android.webkit.JavascriptInterface
import com.victordev.flashcardbrasil.core.service.CardService
import com.google.gson.Gson;
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class CardsBridge(private val cardService: CardService) {

    // Instancia o GSON aqui
    private val gson = Gson()

    @JavascriptInterface
    fun createCard(json: String): String {
        return runBlocking {
            try {
                val data = JSONObject(json)
                val payload = data.getJSONObject("payload")
                val deckId = data.getString("flashCardId")

                cardService.createCard(
                    deckId,
                    payload.getString("question"),
                    payload.getString("answer")
                )
                "{\"status\": 200}"
            } catch (e: Exception) {
                "{\"status\": 500, \"message\": \"${e.message}\"}"
            }
        }
    }

    @JavascriptInterface
    fun getCardsForReview(deckId: String): String {
        return runBlocking {
            val cards = cardService.getCardsForReview(deckId)
            // GSON transforma a lista de objetos em String JSON automaticamente
            gson.toJson(cards)
        }
    }

    @JavascriptInterface
    fun getCards(deckId: String) : String {
        return runBlocking {
            val cards = cardService.getCards(deckId)

            val res = gson.toJson(cards)
            android.util.Log.d("getCards", res)
            res
        }
    }

    @JavascriptInterface
    fun submitReviewdCards(json: String): String {
        return runBlocking {
            try {
                // GSON transforma o JSON que veio do JS em um Array de objetos Kotlin
                val reviews = gson.fromJson(json, Array<CardRateDTO>::class.java)

                reviews.forEach { review ->
                    cardService.updateCardReview(review.cardId, review.rate)
                }
                "{\"status\": 200}"
            } catch (e: Exception) {
                "{\"status\": 500, \"message\": \"${e.localizedMessage}\"}"
            }
        }
    }

    @JavascriptInterface
    fun deleteCard(deckId: String, cardId: String) : Boolean {
        return try {
            runBlocking {
                cardService.deleteCard(deckId, cardId)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}

data class CardRateDTO(
    val cardId: String,
    val rate: Int
)