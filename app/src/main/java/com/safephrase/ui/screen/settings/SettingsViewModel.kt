package com.safephrase.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safephrase.domain.model.ProtectionSettings
import com.safephrase.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {
    val settings: StateFlow<ProtectionSettings> =
        configRepository.protectionSettings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProtectionSettings())

    fun save(settings: ProtectionSettings) = viewModelScope.launch {
        configRepository.saveSettings(settings)
    }
}
