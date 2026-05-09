package com.victordev.flashcardbrasil.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.victordev.flashcardbrasil.dto.DeckDTO
import com.victordev.flashcardbrasil.entidades.ControleEntity
import com.victordev.flashcardbrasil.entidades.DeckEntity

@Dao
interface DeckDao {

    @Insert
    suspend fun insertDeck(deck: DeckEntity)

    @Query("SELECT * FROM controle WHERE `key` = 'controle_flashcards' LIMIT 1")
    suspend fun getControle(): ControleEntity?

    @Query("SELECT * FROM decks ORDER BY flashcardId DESC")
    suspend fun getAllDecks() : List<DeckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertControle(controle: ControleEntity)

    @Query("""
        SELECT d.*,
                (SELECT COUNT(*) FROM cards c WHERE c.flashCardId = d.flashcardId AND c.proximaRevisao <= :hoje) as qtdCartoesPendentes
            FROM decks d
        ORDER BY d.criadoEm DESC
    """)
    suspend fun getAllDecksWithPendingCount(hoje: Int) : List<DeckDTO>
}