package com.hotwheelsgarage.data

import android.content.Context

class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var geminiKey: String
        get() = prefs.getString(KEY_GEMINI, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_GEMINI, value.trim()).apply()

    var geminiModel: String
        get() = prefs.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_GEMINI_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    companion object {
        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        private const val KEY_GEMINI = "gemini_key"
        private const val KEY_MODEL = "gemini_model"
    }
}
