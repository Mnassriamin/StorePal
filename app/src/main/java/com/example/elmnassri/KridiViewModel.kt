package com.example.elmnassri

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KridiViewModel(private val repository: ItemRepository) : ViewModel() {

    // List of all customers
    val allCustomers: StateFlow<List<Customer>> = repository.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Currently selected customer (for detail view)
    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer

    // Logs for the selected customer
    private val _customerLogs = MutableStateFlow<List<CreditLog>>(emptyList())
    val customerLogs: StateFlow<List<CreditLog>> = _customerLogs

    fun selectCustomer(customer: Customer) {
        _selectedCustomer.value = customer
        // Load their history
        viewModelScope.launch {
            repository.getCustomerLogs(customer.id).collect { logs ->
                _customerLogs.value = logs
            }
        }
    }

    fun acceptPayment(amount: Double) = viewModelScope.launch {
        val customer = _selectedCustomer.value
        if (customer != null && amount > 0) {
            repository.payDebt(customer, amount)
            // The list will auto-update because we observe the flow
        }
    }
}

class KridiViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KridiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KridiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}