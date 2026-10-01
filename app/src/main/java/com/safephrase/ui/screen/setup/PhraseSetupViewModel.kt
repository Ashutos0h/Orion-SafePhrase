package com.safephrase.ui.screen.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safephrase.domain.repository.ConfigRepository
import com.safephrase.domain.usecase.SaveSafePhraseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val currentPhrase: String = "",
    val currentLanguage: String = "en",
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PhraseSetupViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val savePhraseUseCase: SaveSafePhraseUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.safePhrase.collect { phrase ->
                _state.update {
                    it.copy(
                        currentPhrase = phrase?.phrase ?: "",
                        currentLanguage = phrase?.language ?: "en"
                    )
                }
            }
        }
    }

    fun savePhrase(phrase: String, language: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        val ok = savePhraseUseCase(phrase.trim(), language.trim().ifBlank { "en" })
        if (ok) {
            _state.update { it.copy(isLoading = false, isSaved = true, error = null) }
        } else {
            _state.update { it.copy(isLoading = false, error = "Phrase must be at least 3 characters long") }
        }
    }
}
