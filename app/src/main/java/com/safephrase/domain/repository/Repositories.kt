package com.safephrase.domain.repository

import com.safephrase.domain.model.*
import kotlinx.coroutines.flow.Flow

/** Monitoring state repository — bridging Service ↔ ViewModel. */
interface MonitoringRepository {
    val serviceState: Flow<ServiceState>
    fun updateState(state: ServiceState)
}

/** Emergency event persistence and sync. */
interface EmergencyRepository {
    suspend fun createEvent(event: EmergencyEvent): String
    suspend fun updateEvent(event: EmergencyEvent)
    fun getAllEvents(): Flow<List<EmergencyEvent>>
    suspend fun getPendingEvents(): List<EmergencyEvent>
    suspend fun sendAlert(event: EmergencyEvent): com.safephrase.core.util.Result<Unit>
}

/** SafePhrase configuration and trusted contacts. */
interface ConfigRepository {
    val safePhrase: Flow<SafePhrase?>
    val trustedContacts: Flow<List<TrustedContact>>
    val protectionSettings: Flow<ProtectionSettings>

    suspend fun saveSafePhrase(phrase: SafePhrase)
    suspend fun saveContact(contact: TrustedContact)
    suspend fun deleteContact(id: String)
    suspend fun saveSettings(settings: ProtectionSettings)
}

/** Auth/user preferences. */
interface PreferencesRepository {
    val onboardingDone: Flow<Boolean>
    val userId: Flow<String>
    val authToken: Flow<String>

    suspend fun markOnboardingDone()
    suspend fun saveAuthToken(token: String)
    suspend fun clearSession()
}
