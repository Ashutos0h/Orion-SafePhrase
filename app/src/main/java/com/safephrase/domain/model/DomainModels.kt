package com.safephrase.domain.model

import java.time.Instant
import java.util.UUID

// ── Service state machine ────────────────────────────────────────────────────

enum class ServiceStatus {
    IDLE,
    STARTING,
    MONITORING,
    SAFE_PHRASE_CANDIDATE,
    VERIFYING,
    EMERGENCY_TRIGGERED,
    CALL_ACTIVE,
    MIC_UNAVAILABLE,
    RECOVERING,
    STOPPED
}

data class ServiceState(
    val status: ServiceStatus = ServiceStatus.IDLE,
    val recoveryAttempt: Int = 0,
    val lastAudioFrameAt: Long = 0L,
    val lastDetectionAt: Long = 0L,
    val isCallActive: Boolean = false,
    val isMicAvailable: Boolean = true
)

// ── Domain entities ─────────────────────────────────────────────────────────

data class SafePhrase(
    val id: String = UUID.randomUUID().toString(),
    val phrase: String,
    val language: String = "en",
    val enabled: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class TrustedContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val email: String = "",
    val notificationToken: String = "",
    val enabled: Boolean = true
)

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestamp: Long,
    val source: LocationSource
)

enum class LocationSource { GPS, NETWORK, LAST_KNOWN, UNAVAILABLE }

enum class EmergencyStatus { PENDING, SENDING, SENT, FAILED }

data class EmergencyEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val triggerType: String = "PHRASE_DETECTED",
    val keywordConfidence: Float = 0f,
    val verificationConfidence: Float = 0f,
    val phraseDetected: String = "",
    val location: LocationResult? = null,
    val status: EmergencyStatus = EmergencyStatus.PENDING,
    val syncStatus: String = "PENDING",
    val retryCount: Int = 0
)

data class ProtectionSettings(
    val keywordConfidenceThreshold: Float = 0.80f,
    val verificationThreshold: Float = 0.75f,
    val cooldownMs: Long = 30_000L,
    val locationTimeoutMs: Long = 5_000L,
    val debugLogging: Boolean = false
)
