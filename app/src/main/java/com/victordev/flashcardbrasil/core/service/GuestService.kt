package com.victordev.flashcardbrasil.core.service

import android.content.Context

class GuestService(
    context : Context
) {

    private val prefs = context.getSharedPreferences("guest_prefs", Context.MODE_PRIVATE)

    private val KEY_IS_GUEST = "is_guest"

    fun saveGuest() {
        prefs.edit().putBoolean(KEY_IS_GUEST, true).apply()
    }

    fun isGuest() : Boolean {
        return prefs.getBoolean(KEY_IS_GUEST, false)
    }

    fun clearGuest() {
        prefs.edit().remove(KEY_IS_GUEST).apply()
    }
}