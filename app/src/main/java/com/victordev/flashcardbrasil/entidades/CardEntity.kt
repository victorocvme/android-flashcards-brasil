package com.victordev.flashcardbrasil.entidades

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey
    val cardId: String,
    val flashCardId: String, // FK para o Deck
    val question: String,
    val answer: String,
    val created_at: String,
    val type: String = "B",
    // Campos do Algoritmo SM-2
    val ef: Double = 2.5,
    val intervalo: Int = 0,
    val repeticoes: Int = 0,
    val proximaRevisao: Int // O índice de dias
)