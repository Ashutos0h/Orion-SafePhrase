package com.safephrase.ui.screen.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safephrase.domain.model.TrustedContact
import com.safephrase.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {

    val contacts: StateFlow<List<TrustedContact>> =
        configRepository.trustedContacts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addContact(name: String, phone: String, email: String) = viewModelScope.launch {
        if (name.isBlank() || phone.isBlank()) return@launch
        configRepository.saveContact(
            TrustedContact(id = UUID.randomUUID().toString(), name = name, phone = phone, email = email)
        )
    }

    fun deleteContact(id: String) = viewModelScope.launch {
        configRepository.deleteContact(id)
    }
}
