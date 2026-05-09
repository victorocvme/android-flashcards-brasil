package com.victordev.flashcardbrasil.config

import androidx.room.Database
import androidx.room.RoomDatabase
import com.victordev.flashcardbrasil.dao.CardDao
import com.victordev.flashcardbrasil.dao.DeckDao
import com.victordev.flashcardbrasil.entidades.DeckEntity
import com.victordev.flashcardbrasil.entidades.CardEntity
import com.victordev.flashcardbrasil.entidades.ControleEntity

@Database(entities = [ DeckEntity::class, ControleEntity::class, CardEntity::class ], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun deckDao() : DeckDao
    abstract fun cardDao() : CardDao
}