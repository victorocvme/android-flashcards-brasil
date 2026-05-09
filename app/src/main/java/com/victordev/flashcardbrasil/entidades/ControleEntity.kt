package com.victordev.flashcardbrasil.entidades
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("controle")
data class ControleEntity(
    @PrimaryKey
    val key: String = "controle_flashcard",
    val qtdFlashcards : Int = 0
)