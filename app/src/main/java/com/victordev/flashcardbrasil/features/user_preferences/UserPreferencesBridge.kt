package com.victordev.flashcardbrasil.features.user_preferences

import android.content.Context
import android.webkit.JavascriptInterface

class UserPreferencesBridge(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @JavascriptInterface
    fun getTheme(): String {
        return prefs.getString(THEME_KEY, DEFAULT_THEME) ?: DEFAULT_THEME
    }

    @JavascriptInterface
    fun setTheme(theme: String): Boolean {
        val normalizedTheme = theme.lowercase()

        if (normalizedTheme != LIGHT_THEME && normalizedTheme != DARK_THEME) {
            return false
        }

        prefs.edit()
            .putString(THEME_KEY, normalizedTheme)
            .apply()

        return true
    }

    private companion object {
        const val PREFS_NAME = "user_preferences"
        const val THEME_KEY = "theme"
        const val DEFAULT_THEME = "light"
        const val LIGHT_THEME = "light"
        const val DARK_THEME = "dark"
    }
}
