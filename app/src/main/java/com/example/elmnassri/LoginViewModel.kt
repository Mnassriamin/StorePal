package com.example.elmnassri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: ItemRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginResult>(LoginResult.Idle)
    val loginState: StateFlow<LoginResult> = _loginState

    init {
        // Create the default "0000" admin if the app is fresh
        viewModelScope.launch {
            repository.createDefaultAdmin()
        }
    }

    fun attemptLogin(pin: String) = viewModelScope.launch {
        if (pin.isBlank()) {
            _loginState.value = LoginResult.Error("Please enter a PIN")
            return@launch
        }

        val user = repository.login(pin)
        if (user != null) {
            UserSession.currentUser = user // Set the session!
            _loginState.value = LoginResult.Success(user)
        } else {
            _loginState.value = LoginResult.Error("Invalid PIN")
        }
    }

    // A sealed class helps us manage the UI states easily
    sealed class LoginResult {
        object Idle : LoginResult()
        data class Success(val user: User) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}

// Update the Factory to support LoginViewModel
class LoginViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}