package com.matchpoint.app.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.matchpoint.app.backend.RegistrationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegistrationUiState(
    val isCheckingAuth: Boolean = true,
    val authFailed: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class RegistrationViewModel(private val repository: RegistrationRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private var userId: String? = null

    init {
        ensureAuth()
    }

    fun ensureAuth() {
        viewModelScope.launch {
            _uiState.value = RegistrationUiState(isCheckingAuth = true)
            val id = repository.ensureAuthenticated()
            userId = id
            _uiState.value = RegistrationUiState(isCheckingAuth = false, authFailed = id == null)
        }
    }

    fun register(name: String, email: String) {
        val id = userId ?: return
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter your name to continue.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            val success = repository.register(id, trimmedName, email.trim().ifBlank { null })
            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                // isRegistered flipping (on success) swaps the screen away via MainActivity;
                // nothing else to do here in that case.
                errorMessage = if (success) null else "Couldn't complete registration. Check your connection and try again."
            )
        }
    }

    class Factory(private val repository: RegistrationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RegistrationViewModel(repository) as T
    }
}
