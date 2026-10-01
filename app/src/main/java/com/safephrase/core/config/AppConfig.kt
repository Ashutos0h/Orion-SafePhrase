package com.safephrase.core.config

/**
 * Central application configuration. All tunable constants live here.
 * Replace with remote-config or BuildConfig fields for production.
 */
object AppConfig {

    // ── Audio ────────────────────────────────────────────────────────────────
    const val SAMPLE_RATE_HZ = 16_000
    const val CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_IN_MONO
    const val AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT
    const val FRAME_SIZE_MS = 30           // ms per processing frame
    const val FRAME_SAMPLES = SAMPLE_RATE_HZ * FRAME_SIZE_MS / 1000 // 480 samples

    // ── Detection ────────────────────────────────────────────────────────────
    const val KEYWORD_CONFIDENCE_THRESHOLD = 0.80f
    const val RISK_VERIFICATION_THRESHOLD  = 0.75f
    const val EMERGENCY_COOLDOWN_MS        = 30_000L   // 30 s between alerts
    const val MAX_MIC_RECOVERY_ATTEMPTS    = 3
    const val MIC_RECOVERY_DELAY_MS        = 2_000L

    // ── Location ─────────────────────────────────────────────────────────────
    const val LOCATION_TIMEOUT_MS          = 5_000L
    const val LOCATION_MAX_AGE_MS          = 60_000L   // 1 min

    // ── Sync / Network ───────────────────────────────────────────────────────
    const val NETWORK_TIMEOUT_S            = 30L
    const val SYNC_RETRY_MAX               = 5
    const val SYNC_BACKOFF_INITIAL_S       = 10L

    // ── Notification ─────────────────────────────────────────────────────────
    const val CHANNEL_ID_MONITORING        = "safephrase_monitoring"
    const val CHANNEL_ID_EMERGENCY         = "safephrase_emergency"
    const val NOTIF_ID_MONITORING          = 1001
    const val NOTIF_ID_EMERGENCY           = 1002

    // ── Preferences keys ─────────────────────────────────────────────────────
    const val PREF_SAFE_PHRASE             = "safe_phrase"
    const val PREF_PHRASE_LANGUAGE         = "phrase_language"
    const val PREF_PROTECTION_ENABLED      = "protection_enabled"
    const val PREF_USER_ID                 = "user_id"
    const val PREF_AUTH_TOKEN              = "auth_token"
    const val PREF_ONBOARDING_DONE         = "onboarding_done"
}
