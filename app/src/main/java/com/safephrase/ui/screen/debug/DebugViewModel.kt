package com.safephrase.ui.screen.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safephrase.domain.model.EmergencyStatus
import com.safephrase.domain.model.ServiceState
import com.safephrase.domain.repository.EmergencyRepository
import com.safephrase.domain.repository.MonitoringRepository
import com.safephrase.service.MicrophoneMonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DebugUiState(
    val serviceState: ServiceState = ServiceState(),
    val pendingCount: Int = 0,
    val totalCount: Int = 0
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    monitoringRepository: MonitoringRepository,
    emergencyRepository: EmergencyRepository
) : ViewModel() {
    val state: StateFlow<DebugUiState> = combine(
        monitoringRepository.serviceState,
        emergencyRepository.getAllEvents()
    ) { svc, events ->
        DebugUiState(
            serviceState = svc,
            pendingCount = events.count { it.status == EmergencyStatus.PENDING },
            totalCount = events.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebugUiState())

    val serviceStateFlow = MicrophoneMonitorService.state
}
