package com.samjhoenglish.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.speech.EnglishVoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.settingsStore

    /** null until the user picks a language on first launch. */
    val appLanguage: Flow<AppLanguage?> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[APP_LANGUAGE]?.let { saved -> AppLanguage.entries.firstOrNull { it.name == saved } }
        }

    suspend fun setAppLanguage(language: AppLanguage) {
        store.edit { it[APP_LANGUAGE] = language.name }
    }

    /** The child currently using the app; null until one is chosen. */
    val currentChildId: Flow<Long?> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[CURRENT_CHILD] }

    suspend fun setCurrentChild(childId: Long) {
        store.edit { it[CURRENT_CHILD] = childId }
    }

    val englishVoice: Flow<EnglishVoice> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> EnglishVoice.entries.firstOrNull { it.name == prefs[ENGLISH_VOICE] } ?: EnglishVoice.AMERICAN }

    suspend fun setEnglishVoice(voice: EnglishVoice) {
        store.edit { it[ENGLISH_VOICE] = voice.name }
    }

    private companion object {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val CURRENT_CHILD = longPreferencesKey("current_child")
        val ENGLISH_VOICE = stringPreferencesKey("english_voice")
    }
}
