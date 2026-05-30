package com.victordev.flashcardbrasil.core.service

import android.util.Log
import com.victordev.flashcardbrasil.config.AppDatabase
import com.victordev.flashcardbrasil.entidades.CardEntity
import com.victordev.flashcardbrasil.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.UUID

class CardService (private val db: AppDatabase) {

    private val cardDao = db.cardDao()
    private val deckDao = db.deckDao()

    suspend fun createCard(deckId: String, question: String, answer: String) : String = withContext(
        Dispatchers.IO
    ) {
        Log.d("DEBUG_CREATEACARD", deckId)
        val newCard = CardEntity(
            cardId = UUID.randomUUID().toString(),
            flashCardId = deckId,
            question = question,
            answer = answer,
            created_at = OffsetDateTime.now().toString(),
            proximaRevisao = DateUtils.getDiasDesdeDataBase(),
            ef = 2.5,
            intervalo = 0,
            repeticoes = 0,
            type = "SM2"
        )

        cardDao.insertCard(newCard)
        cardDao.incrementCardCount(deckId)

        "Sucesso"
    }

    suspend fun getCardsForReview(deckId: String): List<CardEntity> = withContext(Dispatchers.IO) {
        val hoje = DateUtils.getDiasDesdeDataBase()
        cardDao.getCardsForReview(deckId, hoje)
    }

    suspend fun getCards(deckId: String) : List<CardEntity> = withContext(Dispatchers.IO) {
        cardDao.getCards(deckId)
    }

    suspend fun importCards(cards: List<CardEntity>): Int = withContext(Dispatchers.IO) {
        cards.forEach { card ->
            if (card.cardId.isBlank()) {
                throw Exception("ID do card nao pode ser vazio")
            }

            if (card.flashCardId.isBlank()) {
                throw Exception("ID do deck nao pode ser vazio")
            }

            if (card.question.isBlank()) {
                throw Exception("Pergunta do card nao pode ser vazia")
            }

            if (card.answer.isBlank()) {
                throw Exception("Resposta do card nao pode ser vazia")
            }
        }

        cardDao.upsertCards(cards)
        cards.map { it.flashCardId }.distinct().forEach { deckId ->
            deckDao.updateDeckCardsCount(deckId)
        }
        cards.size
    }

    suspend fun upsertCards(cards: List<UpsertCardRequest>): Int = withContext(Dispatchers.IO) {
        if (cards.isEmpty()) {
            return@withContext 0
        }

        val now = OffsetDateTime.now().toString()
        val today = DateUtils.getDiasDesdeDataBase()
        val affectedDeckIds = mutableSetOf<String>()
        val entities = mutableListOf<CardEntity>()

        cards.forEach { card ->
            entities.add(toUpsertEntity(card, now, today, affectedDeckIds))
        }

        cardDao.upsertCards(entities)

        affectedDeckIds.forEach { deckId ->
            deckDao.updateDeckCardsCount(deckId)
        }

        cards.size
    }

    private suspend fun toUpsertEntity(
        card: UpsertCardRequest,
        createdAt: String,
        today: Int,
        affectedDeckIds: MutableSet<String>
    ): CardEntity {
        val deckId = card.flashCardId?.takeIf { it.isNotBlank() }
            ?: throw Exception("ID do deck nao pode ser vazio")

        if (card.question.isBlank()) {
            throw Exception("Pergunta do card nao pode ser vazia")
        }

        if (card.answer.isBlank()) {
            throw Exception("Resposta do card nao pode ser vazia")
        }

        val cardId = card.cardId?.takeIf { it.isNotBlank() }
        val existingCard = cardId?.let { cardDao.getCardById(it) }

        affectedDeckIds.add(deckId)

        return if (existingCard != null) {
            affectedDeckIds.add(existingCard.flashCardId)
            existingCard.copy(
                flashCardId = deckId,
                question = card.question,
                answer = card.answer,
                type = "SM2",
                ef = INITIAL_EF,
                intervalo = INITIAL_INTERVALO,
                repeticoes = INITIAL_REPETICOES,
                proximaRevisao = today
            )
        } else {
            CardEntity(
                cardId = cardId ?: UUID.randomUUID().toString(),
                flashCardId = deckId,
                question = card.question,
                answer = card.answer,
                created_at = createdAt,
                proximaRevisao = today,
                ef = INITIAL_EF,
                intervalo = INITIAL_INTERVALO,
                repeticoes = INITIAL_REPETICOES,
                type = "SM2"
            )
        }
    }

    suspend fun deleteCard(deckId: String, cardId: String) {
        cardDao.deleteCard(deckId, cardId)
        deckDao.updateDeckCardsCount(deckId)
    }

    suspend fun updateCardReview(cardId: String, grade: Int) = withContext(Dispatchers.IO) {
        // 1. Busca o card atual no banco
        val card = cardDao.getCardById(cardId) ?: return@withContext

        // 2. Pega o dia atual (índice baseado na data base)
        val hoje = DateUtils.getDiasDesdeDataBase()

        // 3. Calcula os novos valores (EF, Intervalo, Repetições)
        val updatedCard = applySM2(card, grade, hoje)

        // 4. Salva o card atualizado no banco
        cardDao.updateReview(updatedCard, card.flashCardId)
    }

    private fun applySM2(card: CardEntity, grade: Int, hoje: Int): CardEntity {
        var ef = card.ef
        var intervalo = card.intervalo
        var repeticoes = card.repeticoes

        if (grade >= 3) {
            when (repeticoes) {
                0 -> intervalo = 1
                1 -> intervalo = 6
                else -> intervalo = Math.round(intervalo * ef).toInt()
            }
            repeticoes++
        } else {
            repeticoes = 0
            intervalo = 1
        }

        ef += (0.1 - (5 - grade) * (0.08 + (5 - grade) * 0.02))
        if (ef < 1.3) ef = 1.3

        return card.copy(
            ef = ef,
            intervalo = intervalo,
            repeticoes = repeticoes,
            proximaRevisao = hoje + intervalo
        )
    }


    private companion object {
        const val INITIAL_EF = 2.5
        const val INITIAL_INTERVALO = 0
        const val INITIAL_REPETICOES = 0
    }
}

data class UpsertCardRequest(
    val cardId: String?,
    val flashCardId: String?,
    val question: String,
    val answer: String
)
