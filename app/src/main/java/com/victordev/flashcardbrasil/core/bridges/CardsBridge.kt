package com.victordev.flashcardbrasil.core.bridges

import android.webkit.JavascriptInterface
import com.victordev.flashcardbrasil.core.service.CardService
import com.victordev.flashcardbrasil.core.service.UpsertCardRequest
import com.google.gson.Gson;
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
    fun upsertCards(json: String): String {
        return runBlocking {
            try {
                val cards = parseUpsertCards(json)
                val upsertedCount = cardService.upsertCards(cards)

                "{\"status\": 200, \"upserted\": $upsertedCount}"
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

    private fun parseUpsertCards(json: String): List<UpsertCardRequest> {
        val root = JsonParser.parseString(json)
        val defaultDeckId = root.asJsonObjectOrNull()
            ?.getStringOrNull("flashCardId")
            ?: root.asJsonObjectOrNull()?.getStringOrNull("flashcardId")

        val cardsElement = when {
            root.isJsonArray -> root
            root.asJsonObjectOrNull()?.has("cards") == true -> root.asJsonObject.get("cards")
            else -> throw IllegalArgumentException("JSON deve ser uma lista de cards ou conter a propriedade cards")
        }

        if (!cardsElement.isJsonArray) {
            throw IllegalArgumentException("cards deve ser uma lista")
        }

        return cardsElement.asJsonArray.map { cardElement ->
            val card = cardElement.asJsonObject
            val deckId = card.getStringOrNull("flashCardId")
                ?: card.getStringOrNull("flashcardId")
                ?: defaultDeckId

            UpsertCardRequest(
                cardId = card.getStringOrNull("cardId"),
                flashCardId = deckId,
                question = card.getStringOrNull("question").orEmpty(),
                answer = card.getStringOrNull("answer").orEmpty()
            )
        }
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
        return if (isJsonObject) asJsonObject else null
    }

    private fun JsonObject.getStringOrNull(key: String): String? {
        val value = get(key) ?: return null
        if (value.isJsonNull) return null
        return value.asString.takeIf { it.isNotBlank() }
    }
}

data class CardRateDTO(
    val cardId: String,
    val rate: Int
)
