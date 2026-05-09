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


}