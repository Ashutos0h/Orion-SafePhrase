package com.safephrase.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.safephrase.core.config.AppConfig
import com.safephrase.core.util.SafeLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "safephrase_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val context: Context
) {
    private val store = context.dataStore

    // ── Keys ─────────────────────────────────────────────────────────────────
    private object Keys {
        val SAFE_PHRASE       = stringPreferencesKey(AppConfig.PREF_SAFE_PHRASE)
        val PHRASE_LANGUAGE   = stringPreferencesKey(AppConfig.PREF_PHRASE_LANGUAGE)
        val PROTECTION_ON     = booleanPreferencesKey(AppConfig.PREF_PROTECTION_ENABLED)
        val ONBOARDING_DONE   = booleanPreferencesKey(AppConfig.PREF_ONBOARDING_DONE)
        val USER_ID           = stringPreferencesKey(AppConfig.PREF_USER_ID)
        val KW_THRESHOLD      = floatPreferencesKey("kw_threshold")
        val VERIF_THRESHOLD   = floatPreferencesKey("verif_threshold")
        val COOLDOWN_MS       = longPreferencesKey("cooldown_ms")
    }

    // ── Safe reads (emit empty on IOException) ────────────────────────────────
    val safePhrase: Flow<String> = store.data
        .catchIO().map { it[Keys.SAFE_PHRASE] ?: "" }

    val phraseLanguage: Flow<String> = store.data
        .catchIO().map { it[Keys.PHRASE_LANGUAGE] ?: "en" }

    val onboardingDone: Flow<Boolean> = store.data
        .catchIO().map { it[Keys.ONBOARDING_DONE] ?: false }

    val userId: Flow<String> = store.data
        .catchIO().map { it[Keys.USER_ID] ?: "" }

    val kwThreshold: Flow<Float> = store.data
        .catchIO().map { it[Keys.KW_THRESHOLD] ?: AppConfig.KEYWORD_CONFIDENCE_THRESHOLD }

    val verifThreshold: Flow<Float> = store.data
        .catchIO().map { it[Keys.VERIF_THRESHOLD] ?: AppConfig.RISK_VERIFICATION_THRESHOLD }

    val cooldownMs: Flow<Long> = store.data
        .catchIO().map { it[Keys.COOLDOWN_MS] ?: AppConfig.EMERGENCY_COOLDOWN_MS }

    val userPitchMin: Flow<Float> = store.data.catchIO().map { it[floatPreferencesKey("user_pitch_min")] ?: 80f }
    val userPitchMax: Flow<Float> = store.data.catchIO().map { it[floatPreferencesKey("user_pitch_max")] ?: 280f }

    // ── Writes ────────────────────────────────────────────────────────────────
    suspend fun saveSafePhrase(phrase: String, language: String) = store.edit {
        it[Keys.SAFE_PHRASE] = phrase
        it[Keys.PHRASE_LANGUAGE] = language
    }
    
    suspend fun saveVoiceProfile(minPitch: Float, maxPitch: Float) = store.edit {
        it[floatPreferencesKey("user_pitch_min")] = minPitch
        it[floatPreferencesKey("user_pitch_max")] = maxPitch
    }

    suspend fun markOnboardingDone() = store.edit { it[Keys.ONBOARDING_DONE] = true }

    suspend fun saveUserId(id: String) = store.edit { it[Keys.USER_ID] = id }

    suspend fun saveThresholds(kwThreshold: Float, verifThreshold: Float, cooldownMs: Long) =
        store.edit {
            it[Keys.KW_THRESHOLD]    = kwThreshold
            it[Keys.VERIF_THRESHOLD] = verifThreshold
            it[Keys.COOLDOWN_MS]     = cooldownMs
        }

    suspend fun clearAll() = store.edit { it.clear() }

    private fun <T> Flow<T>.catchIO(): Flow<T> =
        catch { e ->
            if (e is IOException) SafeLogger.e("DataStore", "Read error", e)
            else throw e
        }
}
