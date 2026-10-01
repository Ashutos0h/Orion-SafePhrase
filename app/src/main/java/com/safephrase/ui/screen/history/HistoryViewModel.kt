package com.safephrase.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safephrase.domain.model.EmergencyEvent
import com.safephrase.domain.repository.EmergencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    emergencyRepository: EmergencyRepository
) : ViewModel() {
    val events: StateFlow<List<EmergencyEvent>> =
        emergencyRepository.getAllEvents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
