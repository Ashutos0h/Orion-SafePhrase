package com.safephrase.data.repository

import com.safephrase.core.util.Result
import com.safephrase.core.util.SafeLogger
import com.safephrase.data.local.db.dao.*
import com.safephrase.data.local.db.entity.*
import com.safephrase.data.local.preferences.UserPreferencesDataStore
import com.safephrase.data.remote.api.SafePhraseApi
import com.safephrase.data.remote.dto.EmergencyAlertRequest
import com.safephrase.domain.model.*
import com.safephrase.domain.repository.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "EmergencyRepo"

@Singleton
class EmergencyRepositoryImpl @Inject constructor(
    private val dao: EmergencyEventDao,
    private val api: SafePhraseApi,
    private val prefs: UserPreferencesDataStore
) : EmergencyRepository {

    override suspend fun createEvent(event: EmergencyEvent): String {
        dao.insert(event.toEntity())
        SafeLogger.i(TAG, "Event created: ${event.id}")
        return event.id
    }

    override suspend fun updateEvent(event: EmergencyEvent) =
        dao.update(event.toEntity())

    override fun getAllEvents(): Flow<List<EmergencyEvent>> =
        dao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getPendingEvents(): List<EmergencyEvent> =
        dao.getPending().map { it.toDomain() }

    override suspend fun sendAlert(event: EmergencyEvent): Result<Unit> {
        return try {
            val token = prefs.userId.first()   // replace with actual auth token flow
            val response = api.sendEmergencyAlert(
                token = "Bearer $token",
                request = EmergencyAlertRequest(
                    eventId = event.id,
                    timestamp = event.timestamp,
                    triggerType = event.triggerType,
                    keywordConfidence = event.keywordConfidence,
                    verificationConfidence = event.verificationConfidence,
                    phraseDetected = event.phraseDetected,
                    latitude = event.location?.latitude,
                    longitude = event.location?.longitude,
                    locationAccuracy = event.location?.accuracyMeters,
                    locationSource = event.location?.source?.name
                )
            )
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            SafeLogger.e(TAG, "sendAlert failed", e)
            Result.Error(e.message ?: "Network error", e)
        }
    }

    // ── Mappers ────────────────────────────────────────────────────────────

    private fun EmergencyEvent.toEntity() = EmergencyEventEntity(
        id = id,
        timestamp = timestamp,
        triggerType = triggerType,
        keywordConfidence = keywordConfidence,
        verificationConfidence = verificationConfidence,
        phraseDetected = phraseDetected,
        latitude = location?.latitude,
        longitude = location?.longitude,
        locationAccuracy = location?.accuracyMeters,
        locationTimestamp = location?.timestamp,
        locationSource = location?.source?.name,
        status = status.name,
        syncStatus = syncStatus,
        retryCount = retryCount
    )

    private fun EmergencyEventEntity.toDomain() = EmergencyEvent(
        id = id,
        timestamp = timestamp,
        triggerType = triggerType,
        keywordConfidence = keywordConfidence,
        verificationConfidence = verificationConfidence,
        phraseDetected = phraseDetected,
        location = latitude?.let {
            LocationResult(
                latitude = it,
                longitude = longitude!!,
                accuracyMeters = locationAccuracy ?: 0f,
                timestamp = locationTimestamp ?: 0L,
                source = LocationSource.valueOf(locationSource ?: "LAST_KNOWN")
            )
        },
        status = EmergencyStatus.valueOf(status),
        syncStatus = syncStatus,
        retryCount = retryCount
    )
}

@Singleton
class ConfigRepositoryImpl @Inject constructor(
    private val safePhraseDao: SafePhraseDao,
    private val contactDao: TrustedContactDao,
    private val prefs: UserPreferencesDataStore
) : ConfigRepository {

    override val safePhrase: Flow<SafePhrase?> =
        safePhraseDao.getActiveFlow().map { it?.toDomain() }

    override val trustedContacts: Flow<List<TrustedContact>> =
        contactDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override val protectionSettings: Flow<ProtectionSettings> = combine(
        prefs.kwThreshold,
        prefs.verifThreshold,
        prefs.cooldownMs
    ) { kw, verif, cooldown ->
        ProtectionSettings(
            keywordConfidenceThreshold = kw,
            verificationThreshold = verif,
            cooldownMs = cooldown
        )
    }

    override suspend fun saveSafePhrase(phrase: SafePhrase) =
        safePhraseDao.insert(phrase.toEntity())

    override suspend fun saveContact(contact: TrustedContact) =
        contactDao.insert(contact.toEntity())

    override suspend fun deleteContact(id: String) {
        contactDao.deleteById(id)
    }

    override suspend fun saveSettings(settings: ProtectionSettings) {
        prefs.saveThresholds(
            settings.keywordConfidenceThreshold,
            settings.verificationThreshold,
            settings.cooldownMs
        )
    }

    // ── Mappers ────────────────────────────────────────────────────────────

    private fun SafePhraseEntity.toDomain() = SafePhrase(
        id = id,
        phrase = phrase,
        language = language,
        enabled = enabled,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt)
    )

    private fun SafePhrase.toEntity() = SafePhraseEntity(
        id = id,
        phrase = phrase,
        language = language,
        enabled = enabled,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = System.currentTimeMillis()
    )

    private fun TrustedContactEntity.toDomain() = TrustedContact(
        id = id, name = name, phone = phone, email = email,
        notificationToken = notificationToken, enabled = enabled
    )

    private fun TrustedContact.toEntity() = TrustedContactEntity(
        id = id, name = name, phone = phone, email = email,
        notificationToken = notificationToken, enabled = enabled
    )
}

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val prefs: UserPreferencesDataStore
) : PreferencesRepository {
    override val onboardingDone: Flow<Boolean> = prefs.onboardingDone
    override val userId: Flow<String>          = prefs.userId
    override val authToken: Flow<String>       = prefs.userId // placeholder

    override suspend fun markOnboardingDone() { prefs.markOnboardingDone() }
    override suspend fun saveAuthToken(token: String) { prefs.saveUserId(token) }
    override suspend fun clearSession() { prefs.clearAll() }
}

@Singleton
class MonitoringRepositoryImpl @Inject constructor() : MonitoringRepository {
    private val _state = MutableStateFlow(ServiceState())
    override val serviceState: Flow<ServiceState> = _state.asStateFlow()
    override fun updateState(state: ServiceState) { _state.value = state }
}
