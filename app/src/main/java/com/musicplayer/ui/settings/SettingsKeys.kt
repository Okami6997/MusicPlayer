package com.musicplayer.ui.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
    val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")

    val ANDROID_AUTO_ENABLED = booleanPreferencesKey("android_auto_enabled")
    val ANDROID_AUTO_AUTO_PLAY = booleanPreferencesKey("android_auto_auto_play")
    val ANDROID_AUTO_SHUFFLE_MODE = booleanPreferencesKey("android_auto_shuffle_mode")

    val DOWNLOAD_SOURCE_URL = stringPreferencesKey("download_source_url")

    // New UI Feature Flag
    val USE_NEW_UI = booleanPreferencesKey("use_new_ui")

    // Global last-sync timestamps (epoch millis) for the "Sync" settings screen.
    val LAST_DELTA_SYNC_TIME = longPreferencesKey("last_delta_sync_time")
    val LAST_FULL_SYNC_TIME = longPreferencesKey("last_full_sync_time")
}
