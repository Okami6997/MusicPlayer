package com.musicplayer.ui.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys {
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
    val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
    val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
    val EQUALIZER_BANDS = stringPreferencesKey("equalizer_bands")
    
    val ANDROID_AUTO_ENABLED = booleanPreferencesKey("android_auto_enabled")
    val ANDROID_AUTO_AUTO_PLAY = booleanPreferencesKey("android_auto_auto_play")
    val ANDROID_AUTO_SHUFFLE_MODE = booleanPreferencesKey("android_auto_shuffle_mode")
}
