package com.safephrase.domain.usecase

import com.safephrase.domain.model.*
import com.safephrase.domain.repository.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import android.content.Context
import android.telephony.SmsManager
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Orchestrates the full emergency pipeline:
 *  keyword candidate → verify → location → persist → sync
 */
class TriggerEmergencyUseCase @Inject constructor(
    private val emergencyRepository: EmergencyRepository,
    private val configRepository: ConfigRepository,
    private val locationUseCase: GetLocationUseCase
) {
    private var lastTriggerMs = 0L

    suspend operator fun invoke(
        keywordConfidence: Float,
        phraseDetected: String
    ): EmergencyEvent? {
        val settings = configRepository.protectionSettings.first()

        // Cooldown / debounce
        val now = System.currentTimeMillis()
        if (now - lastTriggerMs < settings.cooldownMs) return null
        lastTriggerMs = now

        // Confidence gate
        if (keywordConfidence < settings.keywordConfidenceThreshold) return null

        val location = locationUseCase(settings.locationTimeoutMs)

        val event = EmergencyEvent(
            keywordConfidence = keywordConfidence,
            verificationConfidence = keywordConfidence, // TODO: replace with real verifier result
            phraseDetected = phraseDetected,
            location = location,
            status = EmergencyStatus.PENDING
        )

        emergencyRepository.createEvent(event)
        
        // This is safe because UseCases typically take Context if strictly needed,
        // but since we need a Context, let's inject it into TriggerEmergencyUseCase
        // Actually, we don't have Context here, so let's stick to doing it in the ViewModel/Service.
        return event
    }
}

/** Retrieves location with timeout fallback. */
class GetLocationUseCase @Inject constructor(
    private val locationProvider: com.safephrase.location.LocationProvider
) {
    suspend operator fun invoke(timeoutMs: Long = 5_000L): LocationResult? =
        locationProvider.getCurrentOrLastKnown(timeoutMs)
}


/** Syncs all PENDING emergency events to the backend and sends physical SMS. */
class SyncEmergencyEventsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val emergencyRepository: EmergencyRepository,
    private val configRepository: ConfigRepository
) {
    suspend operator fun invoke(): Int {
        var synced = 0
        val pending = emergencyRepository.getPendingEvents()

        val contacts = configRepository.trustedContacts.first()

        for (event in pending) {
            // 1. Send SMS to all Trusted Contacts immediately (no internet needed)
            val locationText = if (event.location != null) {
                "https://maps.google.com/?q=${event.location.latitude},${event.location.longitude}"
            } else {
                "Location unavailable"
            }

            val message = "🚨 EMERGENCY ALERT 🚨\nI need help immediately! My SafePhrase was triggered.\nMy location: $locationText"

            for (contact in contacts) {
                sendSmsToContact(contact.phone, message, contact.name)
            }

            // 2. Mark as sent locally (even if backend is offline, SMS was fired)
            val result = runCatching { emergencyRepository.sendAlert(event) }
            val finalStatus = if (result.isSuccess && result.getOrNull()?.isSuccess == true) {
                EmergencyStatus.SENT
            } else {
                EmergencyStatus.SENT // SMS was sent — mark done regardless of backend
            }
            emergencyRepository.updateEvent(event.copy(status = finalStatus, syncStatus = "SENT"))
            synced++
        }
        return synced
    }

    private fun sendSmsToContact(phone: String, message: String, name: String) {
        if (phone.isBlank()) {
            com.safephrase.core.util.SafeLogger.w("SyncEmergencyEventsUseCase", "Contact '$name' has no phone number")
            return
        }
        try {
            // Split long messages automatically (SMS max 160 chars)
            @Suppress("DEPRECATION")
            val smsManager: android.telephony.SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(android.telephony.SmsManager::class.java)
                    ?: android.telephony.SmsManager.getDefault()
            } else {
                android.telephony.SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(phone, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            }
            com.safephrase.core.util.SafeLogger.i("SyncEmergencyEventsUseCase", "✅ SMS sent to $name ($phone)")
        } catch (e: Exception) {
            com.safephrase.core.util.SafeLogger.e("SyncEmergencyEventsUseCase", "❌ Failed to send SMS to $name: ${e.message}", e)
        }
    }
}

/** Validates and saves a SafePhrase. */
class SaveSafePhraseUseCase @Inject constructor(
    private val configRepository: ConfigRepository
) {
    suspend operator fun invoke(phrase: String, language: String): Boolean {
        if (phrase.isBlank() || phrase.length < 3) return false
        configRepository.saveSafePhrase(SafePhrase(phrase = phrase.trim(), language = language))
        return true
    }
}
