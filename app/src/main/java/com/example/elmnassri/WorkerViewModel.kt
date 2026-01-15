package com.example.elmnassri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkerViewModel(private val repository: ItemRepository) : ViewModel() {

    val allWorkers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addWorker(name: String, pin: String) = viewModelScope.launch {
        // Default role is "worker", but you could add a switch for "admin" later
        repository.addWorker(name, pin, "worker")
    }

    fun deleteWorker(user: User) = viewModelScope.launch {
        repository.deleteWorker(user)
    }
}

class WorkerViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}