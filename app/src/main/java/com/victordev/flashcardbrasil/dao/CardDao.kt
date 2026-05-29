package com.victordev.flashcardbrasil.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.victordev.flashcardbrasil.entidades.CardEntity

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCards(cards: List<CardEntity>)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("SELECT * FROM cards WHERE flashCardId = :deckId AND proximaRevisao <= :hojeRelativo LIMIT 10")
    suspend fun getCardsForReview(deckId: String, hojeRelativo: Int): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards WHERE proximaRevisao <= :hojeRelativo")
    suspend fun countCardsForReview(hojeRelativo: Int): Int

    @Query("SELECT * FROM cards WHERE cardId = :cardId")
    suspend fun getCardById(cardId: String): CardEntity?

    @Query("SELECT * FROM cards WHERE flashCardId = :deckId")
    suspend fun getCards(deckId: String) : List<CardEntity>

    @Query("DELETE FROM cards WHERE cardId = :cardId and flashCardId = :deckId")
    suspend fun deleteCard(deckId: String, cardId: String)

    @Transaction
    suspend fun updateReview(card: CardEntity, deckId: String) {
        updateCard(card)
    }

    @Query("UPDATE decks SET qtdCartoes = qtdCartoes + 1 WHERE flashcardId = :deckId")
    suspend fun incrementCardCount(deckId: String)

    @Query("DELETE FROM cards WHERE flashCardId = :deckId")
    suspend fun deleteCardsByDeckId(deckId: String)
}
