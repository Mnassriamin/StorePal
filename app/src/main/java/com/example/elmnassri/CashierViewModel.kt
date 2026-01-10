package com.example.elmnassri

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CashierViewModel(private val repository: ItemRepository) : ViewModel() {

    // We use OrderItem directly so it matches the Adapter
    private val _basket = MutableStateFlow<List<OrderItem>>(emptyList())
    val basket: StateFlow<List<OrderItem>> = _basket.asStateFlow()

    private val _totalPrice = MutableStateFlow(0.0)
    val totalPrice: StateFlow<Double> = _totalPrice.asStateFlow()

    fun addItemByBarcode(barcode: String) = viewModelScope.launch {
        val currentList = _basket.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.barcode == barcode }

        if (existingIndex != -1) {
            // 1. Item exists? Increase quantity by 1
            val existingItem = currentList[existingIndex]
            currentList[existingIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
            updateBasket(currentList)
        } else {
            // 2. New item? Fetch from DB and convert to OrderItem
            val itemFromDb = repository.getItemByBarcode(barcode)
            if (itemFromDb != null) {
                val newItem = OrderItem(
                    barcode = itemFromDb.barcode,
                    itemName = itemFromDb.name,
                    priceAtSale = itemFromDb.price,
                    quantity = 1
                )
                currentList.add(newItem)
                updateBasket(currentList)
            } else {
                Log.e("Cashier", "Item with barcode $barcode not found")
            }
        }
    }

    // Manual Quantity Edit
    fun updateQuantity(item: OrderItem, newQuantity: Int) {
        val currentList = _basket.value.toMutableList()
        val index = currentList.indexOfFirst { it.barcode == item.barcode }

        if (index != -1) {
            if (newQuantity > 0) {
                currentList[index] = item.copy(quantity = newQuantity)
                updateBasket(currentList)
            } else {
                // If they enter 0, remove the item
                removeItem(item)
            }
        }
    }

    fun removeItem(item: OrderItem) {
        val currentList = _basket.value.toMutableList()
        currentList.remove(item)
        updateBasket(currentList)
    }

    fun clearBasket() {
        updateBasket(emptyList())
    }

    fun submitOrder() = viewModelScope.launch {
        val currentItems = _basket.value
        if (currentItems.isNotEmpty()) {
            val total = _totalPrice.value

            // Save to repository
            repository.saveOrder(total, currentItems)

            // Clear screen
            clearBasket()
        }
    }

    private fun updateBasket(newList: List<OrderItem>) {
        _basket.value = newList
        calculateTotal()
    }

    private fun calculateTotal() {
        var sum = 0.0
        for (item in _basket.value) {
            sum += (item.priceAtSale * item.quantity)
        }
        _totalPrice.value = sum
    }
}

// --- THIS WAS MISSING AND CAUSED THE ERROR ---
class CashierViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CashierViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CashierViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}