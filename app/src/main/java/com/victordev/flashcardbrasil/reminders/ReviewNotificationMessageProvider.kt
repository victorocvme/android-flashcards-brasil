package com.victordev.flashcardbrasil.reminders

import android.content.Context
import kotlin.random.Random

class ReviewNotificationMessageProvider(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun nextMessage(count: Int): ReviewNotificationMessage {
        val lastIndex = prefs.getInt(KEY_LAST_INDEX, NO_LAST_INDEX)
        val nextIndex = drawNextIndex(lastIndex)

        prefs.edit()
            .putInt(KEY_LAST_INDEX, nextIndex)
            .apply()

        val template = templates[nextIndex]
        return ReviewNotificationMessage(
            title = template.title,
            text = template.text.replace(COUNT_PLACEHOLDER, count.toString())
        )
    }

    private fun drawNextIndex(lastIndex: Int): Int {
        if (templates.size == 1) return 0

        var index: Int
        do {
            index = Random.nextInt(templates.size)
        } while (index == lastIndex)

        return index
    }

    private companion object {
        const val PREFS_NAME = "review_notification_message_prefs"
        const val KEY_LAST_INDEX = "last_notification_message_index"
        const val NO_LAST_INDEX = -1
        const val COUNT_PLACEHOLDER = "{count}"

        val templates = listOf(
            ReviewNotificationMessage(
                title = "Você ainda lembra disso?",
                text = "Sua revisão está pronta: {count} cartões pendentes."
            ),
            ReviewNotificationMessage(
                title = "Vai deixar esse conteúdo escapar?",
                text = "Volte por alguns minutos e revise seus {count} cartões."
            ),
            ReviewNotificationMessage(
                title = "E se você testasse sua memória agora?",
                text = "{count} cartões estão prontos para revisão."
            ),
            ReviewNotificationMessage(
                title = "Será que isso ainda está fresco na mente?",
                text = "Revise agora seus {count} cartões pendentes."
            ),
            ReviewNotificationMessage(
                title = "Quer evitar esquecer o que estudou?",
                text = "Você tem {count} cartões esperando por revisão."
            ),
            ReviewNotificationMessage(
                title = "A curva do esquecimento chegou",
                text = "Você tem {count} cartões prontos para revisar."
            ),
            ReviewNotificationMessage(
                title = "Seu cérebro pediu reforço",
                text = "{count} cartões estão esperando para fixar de vez."
            ),
            ReviewNotificationMessage(
                title = "O conteúdo começa a escapar",
                text = "Faça uma revisão rápida: {count} cartões pendentes."
            ),
            ReviewNotificationMessage(
                title = "Você aprendeu. Agora precisa manter.",
                text = "Revise seus {count} cartões antes que eles esfriem."
            ),
            ReviewNotificationMessage(
                title = "Poucos minutos salvam o que você estudou",
                text = "Seus {count} cartões estão prontos para revisão."
            )
        )
    }
}
