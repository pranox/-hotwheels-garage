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

    // ---- Drop alerts ----

    var alertsEnabled: Boolean
        get() = prefs.getBoolean("alerts_enabled", false)
        set(value) = prefs.edit().putBoolean("alerts_enabled", value).apply()

    /** How often to check for drops, in minutes. Android can't run periodic work more often than every 15. */
    var checkIntervalMinutes: Int
        get() = prefs.getInt("check_interval", DEFAULT_INTERVAL_MINUTES).coerceAtLeast(MIN_INTERVAL_MINUTES)
        set(value) = prefs.edit().putInt("check_interval", value.coerceAtLeast(MIN_INTERVAL_MINUTES)).apply()

    /** No checks between these hours (local time). -1 = quiet hours off. */
    var quietStartHour: Int
        get() = prefs.getInt("quiet_start", 23)
        set(value) = prefs.edit().putInt("quiet_start", value).apply()

    var quietEndHour: Int
        get() = prefs.getInt("quiet_end", 7)
        set(value) = prefs.edit().putInt("quiet_end", value).apply()

    var salesHourReminder: Boolean
        get() = prefs.getBoolean("sales_hour_reminder", true)
        set(value) = prefs.edit().putBoolean("sales_hour_reminder", value).apply()

    /** Comma-separated subreddit names to watch. */
    var subreddits: String
        get() = prefs.getString("subreddits", null) ?: "HotWheelsIndia"
        set(value) = prefs.edit().putString("subreddits", value.trim()).apply()

    /** Comma-separated public Telegram channel usernames (without @). */
    var telegramChannels: String
        get() = prefs.getString("telegram_channels", "").orEmpty()
        set(value) = prefs.edit().putString("telegram_channels", value.trim()).apply()

    /** Comma-separated words that mean "near me", e.g. city and area names. */
    var locationKeywords: String
        get() = prefs.getString("location_keywords", null) ?: "Bangalore, Bengaluru, BLR, Electronic City, E City, Ecity"
        set(value) = prefs.edit().putString("location_keywords", value.trim()).apply()

    var lastCheckAt: Long
        get() = prefs.getLong("last_check_at", 0)
        set(value) = prefs.edit().putLong("last_check_at", value).apply()

    /** Newest post time already handled per source, so each post alerts at most once. */
    fun lastSeen(source: String): Long? = prefs.getLong("seen_$source", -1).takeIf { it >= 0 }
    fun setLastSeen(source: String, time: Long) = prefs.edit().putLong("seen_$source", time).apply()

    /** Current Treasure Hunt names, refreshed once a day: "yyyy-mm-dd|name1\nname2". */
    var huntNamesCache: String
        get() = prefs.getString("hunt_names", "").orEmpty()
        set(value) = prefs.edit().putString("hunt_names", value).apply()

    fun isQuietNow(hour: Int): Boolean {
        val start = quietStartHour
        val end = quietEndHour
        if (start < 0 || start == end) return false
        return if (start < end) hour in start until end else hour >= start || hour < end
    }

    companion object {
        const val MIN_INTERVAL_MINUTES = 15
        const val DEFAULT_INTERVAL_MINUTES = 30
        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        private const val KEY_GEMINI = "gemini_key"
        private const val KEY_MODEL = "gemini_model"
    }
}
