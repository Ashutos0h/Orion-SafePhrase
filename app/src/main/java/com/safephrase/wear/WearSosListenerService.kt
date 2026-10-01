package com.safephrase.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.safephrase.core.util.SafeLogger
import com.safephrase.core.util.SmsDispatcher
import com.safephrase.domain.repository.ConfigRepository
import com.safephrase.domain.usecase.TriggerEmergencyUseCase
import com.safephrase.service.NotificationHelper
import com.safephrase.sync.AlertSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "WearSosListenerService"
const val WEAR_SOS_PATH = "/safephrase/sos"

/**
 * Runs on the phone, listens for SOS messages sent from the paired Wear OS watch.
 * When the user presses SOS on their watch, this service:
 *  1. Fetches GPS location
 *  2. Fires SMS to all trusted contacts
 *  3. Shows a Heads-Up notification
 *  4. Enqueues backend sync
 */
@AndroidEntryPoint
class WearSosListenerService : WearableListenerService() {

    @Inject lateinit var triggerEmergencyUseCase: TriggerEmergencyUseCase
    @Inject lateinit var configRepository: ConfigRepository

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onMessageReceived(event: MessageEvent) {
        super.onMessageReceived(event)
        if (event.path != WEAR_SOS_PATH) return

        SafeLogger.i(TAG, "⌚ SOS received from Wear OS watch! Node: ${event.sourceNodeId}")

        scope.launch {
            try {
                val event = triggerEmergencyUseCase(1.0f, "WEAR_SOS")
                if (event != null) {
                    val contacts = configRepository.trustedContacts.first()
                    val lat = event.location?.latitude
                    val lng = event.location?.longitude

                    contacts.forEach { contact ->
                        SmsDispatcher.sendEmergencySms(
                            context = applicationContext,
                            phone = contact.phone,
                            name = contact.name,
                            lat = lat,
                            lng = lng
                        )
                    }

                    NotificationHelper.fireEmergencyTriggeredNotification(applicationContext)
                    AlertSyncWorker.enqueue(applicationContext)
                    SafeLogger.i(TAG, "✅ Watch SOS processed — SMS sent to ${contacts.size} contacts")
                }
            } catch (e: Exception) {
                SafeLogger.e(TAG, "❌ Error handling watch SOS", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
