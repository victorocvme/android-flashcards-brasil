package com.victordev.flashcardbrasil.utils

object DateUtils {
    private const val DATA_BASE_ISO = "2026-01-01T00:00:00"

    fun getDiasDesdeDataBase(diasAdicionais: Int = 0): Int {
        val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
        val dataBase = java.time.LocalDateTime.parse(DATA_BASE_ISO, formatter)
        val hoje = java.time.LocalDateTime.now()

        val dias = java.time.temporal.ChronoUnit.DAYS.between(dataBase, hoje)
        return dias.toInt() + diasAdicionais
    }
}