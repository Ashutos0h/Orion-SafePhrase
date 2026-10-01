package com.safephrase.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safephrase.domain.model.*
import com.safephrase.domain.repository.*
import com.safephrase.domain.usecase.TriggerEmergencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import com.safephrase.service.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext

data class DashboardUiState(
    val serviceState: ServiceState = ServiceState(),
    val safePhrase: SafePhrase? = null,
    val contactCount: Int = 0,
    val pendingAlerts: Int = 0,
    val isOnline: Boolean = true
)


@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    monitoringRepository: MonitoringRepository,
    private val configRepository: ConfigRepository,
    emergencyRepository: EmergencyRepository,
    private val triggerEmergencyUseCase: TriggerEmergencyUseCase
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        monitoringRepository.serviceState,
        configRepository.safePhrase,
        configRepository.trustedContacts,
        emergencyRepository.getAllEvents()
    ) { svcState, phrase, contacts, events ->
        DashboardUiState(
            serviceState = svcState,
            safePhrase = phrase,
            contactCount = contacts.size,
            pendingAlerts = events.count { it.status == EmergencyStatus.PENDING }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun triggerManualSOS() {
        viewModelScope.launch {
            val event = triggerEmergencyUseCase(1.0f, "MANUAL_SOS")
            if (event != null) {
                // Fire SMS directly — no WorkManager delay
                val contacts = configRepository.trustedContacts.first()
                val lat = event.location?.latitude
                val lng = event.location?.longitude
                contacts.forEach { contact ->
                    com.safephrase.core.util.SmsDispatcher.sendEmergencySms(
                        context = context,
                        phone = contact.phone,
                        name = contact.name,
                        lat = lat,
                        lng = lng
                    )
                }
                NotificationHelper.fireEmergencyTriggeredNotification(context)
            }
        }
    }
}

