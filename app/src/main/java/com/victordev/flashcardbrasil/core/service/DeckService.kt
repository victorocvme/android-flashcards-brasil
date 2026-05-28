package com.victordev.flashcardbrasil.core.service

import com.victordev.flashcardbrasil.config.AppDatabase
import com.victordev.flashcardbrasil.dto.DeckDTO
import com.victordev.flashcardbrasil.entidades.DeckEntity
import com.victordev.flashcardbrasil.enum.MetodoRevisao
import com.victordev.flashcardbrasil.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID

class DeckService(private val db: AppDatabase) {

    suspend fun deleteDeck(deckId : String) : Boolean {
        return try {
            db.cardDao().deleteCardsByDeckId(deckId)
            val res = db.deckDao().deleteDeckById(deckId)
            res > 0
        } catch (e: Exception){
            false
        }
    }
    suspend fun createDeck(titulo: String, metodoRevisao: String): String {
        validate(titulo, metodoRevisao)

        return withContext(Dispatchers.IO) {
            // Gerando o ID único
            val deckId = UUID.randomUUID().toString()

            val nowIso = LocalDateTime.now().toString()

            val deck = DeckEntity(
                flashcardId = deckId,
                titulo = titulo,
                metodoRevisao = metodoRevisao,
                criadoEm = nowIso,
                syncStatus = "PENDING",
                updatedAt = nowIso,
                qtdCartoes = 0
            )

            db.deckDao().insertDeck(deck)

            deckId // Retorna o UUID para o frontend
        }
    }
    suspend fun getAllDecks() : List<DeckDTO> {
        val hoje = DateUtils.getDiasDesdeDataBase()
        return db.deckDao().getAllDecksWithPendingCount(hoje)
    }

    suspend fun importDecks(decks: List<DeckEntity>): Int {
        decks.forEach { deck ->
            if (deck.flashcardId.isBlank()) {
                throw Exception("ID do deck nao pode ser vazio")
            }

            validate(deck.titulo, deck.metodoRevisao)
        }

        return withContext(Dispatchers.IO) {
            db.deckDao().upsertDecks(decks)
            decks.size
        }
    }

    private fun validate(titulo: String, metodoRevisao: String) {
        if (titulo.length > 60) {
            throw Exception("Título não pode ser maior que 60")
        }

        if (!MetodoRevisao.existe(metodoRevisao)) {
            throw Exception("Método de revisão inválido")
        }
    }
}
