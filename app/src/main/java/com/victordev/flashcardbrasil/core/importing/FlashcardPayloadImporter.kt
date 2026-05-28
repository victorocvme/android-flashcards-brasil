package com.victordev.flashcardbrasil.core.importing

import android.util.Log
import com.victordev.flashcardbrasil.core.service.CardService
import com.victordev.flashcardbrasil.core.service.DeckService
import com.victordev.flashcardbrasil.entidades.CardEntity
import com.victordev.flashcardbrasil.entidades.DeckEntity
import com.victordev.flashcardbrasil.utils.DateUtils
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.OffsetDateTime

class FlashcardPayloadImporter(
    private val deckService: DeckService,
    private val cardService: CardService
) {

    suspend fun importPayload(json: String): ImportResult {
        val payload = JSONObject(json)
        val type = payload.getRequiredString("type")

        return when (type) {
            TYPE_DECKS -> importDecks(payload.getJSONArray("decks"))
            TYPE_CARDS -> importCards(payload.getJSONArray("cards"))
            else -> throw IllegalArgumentException("Tipo de importacao nao suportado: $type")
        }
    }

    private suspend fun importDecks(decksJson: JSONArray): ImportResult {
        val nowIso = LocalDateTime.now().toString()
        val decks = (0 until decksJson.length()).map { index ->
            val deckJson = decksJson.getJSONObject(index)

            DeckEntity(
                flashcardId = deckJson.getRequiredString("flashcardId"),
                titulo = deckJson.getRequiredString("titulo"),
                metodoRevisao = deckJson.getOptionalString("metodo_revisao")
                    ?: deckJson.getRequiredString("metodoRevisao"),
                qtdCartoes = deckJson.optInt("qtdCartoes", 0),
                criadoEm = deckJson.getOptionalString("criadoEm") ?: nowIso,
                syncStatus = deckJson.getOptionalString("syncStatus") ?: "PENDING",
                updatedAt = deckJson.getOptionalString("updatedAt") ?: nowIso
            )
        }

        val importedCount = deckService.importDecks(decks)
        return ImportResult(type = TYPE_DECKS, importedCount = importedCount)
    }

    private suspend fun importCards(cardsJson: JSONArray): ImportResult {
        Log.d(CARD_IMPORT_TAG, "Recebidos ${cardsJson.length()} card(s) para importacao")

        val today = DateUtils.getDiasDesdeDataBase()
        val nowIso = OffsetDateTime.now().toString()
        val cards = (0 until cardsJson.length()).map { index ->
            val cardJson = cardsJson.getJSONObject(index)

            CardEntity(
                cardId = cardJson.getRequiredString("cardId"),
                flashCardId = cardJson.getRequiredString("flashCardId"),
                question = cardJson.getRequiredString("question"),
                answer = cardJson.getRequiredString("answer"),
                created_at = cardJson.getOptionalString("created_at") ?: nowIso,
                type = cardJson.getOptionalString("type") ?: "SM2",
                ef = cardJson.optDouble("ef", 2.5),
                intervalo = cardJson.optInt("intervalo", 0),
                repeticoes = cardJson.optInt("repeticoes", 0),
                proximaRevisao = cardJson.optInt("proximaRevisao", today)
            )
        }

        val importedCount = cardService.importCards(cards)
        Log.d(CARD_IMPORT_TAG, "Importacao concluida: $importedCount card(s)")
        return ImportResult(type = TYPE_CARDS, importedCount = importedCount)
    }

    private fun JSONObject.getRequiredString(name: String): String {
        val value = getOptionalString(name)
        require(!value.isNullOrBlank()) { "Campo obrigatorio ausente: $name" }
        return value
    }

    private fun JSONObject.getOptionalString(name: String): String? {
        if (!has(name) || isNull(name)) {
            return null
        }

        return optString(name).trim().ifBlank { null }
    }

    companion object {
        private const val TYPE_DECKS = "decks"
        private const val TYPE_CARDS = "cards"
        private const val CARD_IMPORT_TAG = "CardImport"
    }
}

data class ImportResult(
    val type: String,
    val importedCount: Int
)
