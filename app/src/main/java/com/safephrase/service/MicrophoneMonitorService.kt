package com.safephrase.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.safephrase.core.config.AppConfig
import com.safephrase.core.util.SafeLogger
import com.safephrase.domain.model.ServiceState
import com.safephrase.domain.model.ServiceStatus
import com.safephrase.domain.repository.ConfigRepository
import com.safephrase.domain.repository.MonitoringRepository
import com.safephrase.domain.usecase.TriggerEmergencyUseCase
import com.safephrase.sync.AlertSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val TAG = "MicrophoneMonitorService"

/**
 * Industry-grade foreground service using Android's native Speech Recognizer (ASR).
 * Runs continuously in the background, listening for the user's SafePhrase.
 * Triggers the emergency pipeline when the phrase is detected.
 */
@AndroidEntryPoint
class MicrophoneMonitorService : Service() {

    companion object {
        const val ACTION_START = "com.safephrase.action.START_PROTECTION"
        const val ACTION_STOP  = "com.safephrase.action.STOP_PROTECTION"

        private val _state = MutableStateFlow(ServiceState())
        val state = _state.asStateFlow()
    }

    @Inject lateinit var triggerEmergency: TriggerEmergencyUseCase
    @Inject lateinit var monitoringRepository: MonitoringRepository
    @Inject lateinit var configRepository: ConfigRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var callMonitor: CallStateMonitor
    private var speechRecognizer: com.safephrase.audio.ContinuousSpeechRecognizer? = null
    private var recoveryAttempts = 0
    private var wakeLock: PowerManager.WakeLock? = null

    // ── Service entry points ──────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafePhrase::MicWakeLock")
        wakeLock?.acquire(24 * 60 * 60 * 1000L /* 24 hours max */)

        callMonitor = CallStateMonitor(
            context = this,
            onCallStarted = ::handleCallStarted,
            onCallEnded   = ::handleCallEnded
        )
        callMonitor.register()
        SafeLogger.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> doStart()
            ACTION_STOP  -> doStop()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        callMonitor.unregister()
        speechRecognizer?.stop()
        speechRecognizer = null
        serviceJob.cancel()
        setStatus(ServiceStatus.STOPPED)
        SafeLogger.i(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Core logic ────────────────────────────────────────────────────────────

    private fun doStart() {
        val notification = NotificationHelper.buildMonitoringNotification(this, ServiceStatus.STARTING)
        ServiceCompat.startForeground(this, AppConfig.NOTIF_ID_MONITORING, notification, NotificationHelper.foregroundServiceType())
        setStatus(ServiceStatus.STARTING)

        serviceScope.launch {
            val config = configRepository.safePhrase.first()

            val language = config?.language ?: "en"
            val targetPhrase = config?.phrase?.lowercase()?.trim() ?: ""

            if (targetPhrase.isBlank()) {
                SafeLogger.e(TAG, "No phrase configured")
                setStatus(ServiceStatus.MIC_UNAVAILABLE)
                updateNotification(ServiceStatus.MIC_UNAVAILABLE)
                return@launch
            }

            SafeLogger.i(TAG, "Starting ASR for phrase: '$targetPhrase' in '$language'")

            speechRecognizer = com.safephrase.audio.ContinuousSpeechRecognizer(this@MicrophoneMonitorService, language)
            speechRecognizer?.start()

            setStatus(ServiceStatus.MONITORING)
            updateNotification(ServiceStatus.MONITORING)
            SafeLogger.i(TAG, "SpeechRecognizer started")

            speechRecognizer?.recognizedText?.collect { text ->
                val detectedText = text.lowercase().trim()
                SafeLogger.d(TAG, "Recognized: '$detectedText'")

                if (detectedText.contains(targetPhrase)) {
                    SafeLogger.i(TAG, "SafePhrase MATCHED! Triggering emergency...")
                    setStatus(ServiceStatus.EMERGENCY_TRIGGERED)
                    updateNotification(ServiceStatus.EMERGENCY_TRIGGERED)

                    val event = triggerEmergency(0.95f, targetPhrase)
                    if (event != null) {
                        // ── Fire SMS immediately via cellular (no internet needed) ──
                        val contacts = configRepository.trustedContacts.first()
                        val lat = event.location?.latitude
                        val lng = event.location?.longitude

                        contacts.forEach { contact ->
                            com.safephrase.core.util.SmsDispatcher.sendEmergencySms(
                                context = this@MicrophoneMonitorService,
                                phone = contact.phone,
                                name = contact.name,
                                lat = lat,
                                lng = lng
                            )
                        }

                        // ── Fire Heads-Up Notification ──
                        NotificationHelper.fireEmergencyTriggeredNotification(this@MicrophoneMonitorService)
                        // ── Enqueue backend sync (best-effort) ──
                        AlertSyncWorker.enqueue(applicationContext)
                        SafeLogger.i(TAG, "Emergency triggered — SMS sent to ${contacts.size} contact(s)")

                        // Cooldown then resume listening
                        speechRecognizer?.stop()
                        delay(AppConfig.EMERGENCY_COOLDOWN_MS)
                        speechRecognizer?.start()
                        setStatus(ServiceStatus.MONITORING)
                        updateNotification(ServiceStatus.MONITORING)
                    }
                }
            }
        }
    }

    private fun doStop() {
        speechRecognizer?.stop()
        speechRecognizer = null
        setStatus(ServiceStatus.STOPPED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Call state ──────────────────────────────────────────────────────────

    private fun handleCallStarted() {
        SafeLogger.i(TAG, "Call started — pausing ASR")
        speechRecognizer?.stop()
        setStatus(ServiceStatus.CALL_ACTIVE, isCallActive = true)
        updateNotification(ServiceStatus.CALL_ACTIVE)
    }

    private fun handleCallEnded() {
        SafeLogger.i(TAG, "Call ended — attempting ASR recovery")
        setStatus(ServiceStatus.RECOVERING, isCallActive = false)
        updateNotification(ServiceStatus.RECOVERING)
        attemptRecovery()
    }

    private fun attemptRecovery() {
        serviceScope.launch {
            recoveryAttempts = 0
            repeat(AppConfig.MAX_MIC_RECOVERY_ATTEMPTS) { attempt ->
                delay(AppConfig.MIC_RECOVERY_DELAY_MS * (attempt + 1))
                try {
                    speechRecognizer?.start()
                    recoveryAttempts = 0
                    setStatus(ServiceStatus.MONITORING)
                    updateNotification(ServiceStatus.MONITORING)
                    SafeLogger.i(TAG, "ASR recovered after ${attempt + 1} attempt(s)")
                    return@launch
                } catch (e: Exception) {
                    recoveryAttempts = attempt + 1
                    SafeLogger.w(TAG, "Recovery attempt ${attempt + 1} failed")
                }
            }
            setStatus(ServiceStatus.MIC_UNAVAILABLE)
            updateNotification(ServiceStatus.MIC_UNAVAILABLE)
            SafeLogger.e(TAG, "ASR recovery failed after ${AppConfig.MAX_MIC_RECOVERY_ATTEMPTS} attempts")
        }
    }

    // ── State helpers ────────────────────────────────────────────────────────

    private fun setStatus(status: ServiceStatus, isCallActive: Boolean = _state.value.isCallActive) {
        val next = _state.value.copy(
            status = status,
            recoveryAttempt = recoveryAttempts,
            isCallActive = isCallActive,
            isMicAvailable = status != ServiceStatus.MIC_UNAVAILABLE
        )
        _state.value = next
        monitoringRepository.updateState(next)
    }

    private fun updateNotification(status: ServiceStatus) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(AppConfig.NOTIF_ID_MONITORING, NotificationHelper.buildMonitoringNotification(this, status))
    }
}
