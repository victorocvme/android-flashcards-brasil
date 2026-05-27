package com.victordev.flashcardbrasil.entidades

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity (
    @PrimaryKey
    val flashcardId: String,
    val titulo : String,
    val metodoRevisao : String,
    val qtdCartoes : Int = 0,
    val criadoEm : String,
    val syncStatus : String,
    val updatedAt : String

)