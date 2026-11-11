package com.samjhoenglish.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.speech.EnglishVoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom

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

    val chimesOn: Flow<Boolean> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[CHIMES_ON] ?: true }

    suspend fun setChimesOn(on: Boolean) {
        store.edit { it[CHIMES_ON] = on }
    }

    // ---------- Kid Mode ----------

    /** The child the phone has been handed to; null when Kid Mode is off. */
    val kidModeChild: Flow<Long?> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[KID_MODE_CHILD] }

    suspend fun setKidModeChild(childId: Long?) {
        store.edit { if (childId == null) it.remove(KID_MODE_CHILD) else it[KID_MODE_CHILD] = childId }
    }

    /** Package names a parent has chosen to lock during Kid Mode. */
    val lockedApps: Flow<Set<String>> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[LOCKED_APPS].orEmpty() }

    suspend fun setLockedApps(packages: Set<String>) {
        store.edit { it[LOCKED_APPS] = packages }
    }

    // ---------- Parent PIN: stored hashed, never as the digits themselves ----------

    val hasPin: Flow<Boolean> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[PIN_HASH] != null }

    suspend fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        store.edit {
            it[PIN_SALT] = salt
            it[PIN_HASH] = hash(salt, pin)
        }
    }

    suspend fun checkPin(pin: String): Boolean {
        val prefs = store.data.first()
        val salt = prefs[PIN_SALT] ?: return false
        return prefs[PIN_HASH] == hash(salt, pin)
    }

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).toHex()

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    // ---------- Extra time a parent granted today ----------

    suspend fun extraMinutes(childId: Long, day: Long): Int {
        val (savedDay, minutes) = store.data.first()[extraKey(childId)]?.split(":") ?: return 0
        return if (savedDay.toLong() == day) minutes.toInt() else 0
    }

    suspend fun addExtraMinutes(childId: Long, day: Long, minutes: Int) {
        val total = extraMinutes(childId, day) + minutes
        store.edit { it[extraKey(childId)] = "$day:$total" }
    }

    private fun extraKey(childId: Long) = stringPreferencesKey("extra_minutes_$childId")

    private companion object {
        val KID_MODE_CHILD = longPreferencesKey("kid_mode_child")
        val LOCKED_APPS = stringSetPreferencesKey("locked_apps")
        val CHIMES_ON = booleanPreferencesKey("chimes_on")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val CURRENT_CHILD = longPreferencesKey("current_child")
        val ENGLISH_VOICE = stringPreferencesKey("english_voice")
    }
}
