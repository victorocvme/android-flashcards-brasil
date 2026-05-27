package com.victordev.flashcardbrasil.enum
enum class MetodoRevisao(val value: String) {
    SM2("SM2"),
    BASICO("BASICO");

    companion object {
        fun existe(valor: String): Boolean {
            return entries.any { it.value == valor }
        }

        fun from(valor: String): MetodoRevisao {
            return entries.find { it.value == valor }
                ?: throw IllegalArgumentException("Método inválido")
        }
    }
}