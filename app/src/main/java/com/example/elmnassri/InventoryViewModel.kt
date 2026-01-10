package com.example.elmnassri

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: ItemRepository) : ViewModel() {

    init {
        repository.startRealtimeSync()
    }

    // Holds the current search query
    private val _searchQuery = MutableStateFlow("")

    // Transforms the search query Flow into a Flow of the filtered item list
    val allItems: Flow<List<Item>> = _searchQuery.flatMapLatest { query ->
        repository.getItems(query)
    }

    /**
     * Updates the search query.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun upsertItem(item: Item, imageUri: Uri?) = viewModelScope.launch {
        repository.upsertItem(item, imageUri)
    }

    fun deleteItem(item: Item) = viewModelScope.launch {
        repository.deleteItem(item)
    }

    suspend fun findItemByBarcode(barcode: String): Item? {
        return repository.getItemByBarcode(barcode)
    }
}

/**
 * Factory for creating a ViewModel with a constructor that takes an ItemRepository.
 */
class InventoryViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository) as T
        }
        // ADD THIS BLOCK:
        if (modelClass.isAssignableFrom(CashierViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CashierViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}