package com.example.elmnassri

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminViewModel(private val repository: ItemRepository) : ViewModel() {

    // --- 1. MONTHLY LOGIC ---
    private val _selectedMonthRange = MutableStateFlow<Pair<Long, Long>>(getCurrentMonthRange())
    private val _currentMonthName = MutableStateFlow<String>(getCurrentMonthName())
    val currentMonthName: StateFlow<String> = _currentMonthName

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyIncome: StateFlow<Double> = _selectedMonthRange.flatMapLatest { (start, end) ->
        repository.getOrdersByDate(start, end)
    }.map { orders ->
        orders.sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // --- 2. DAILY LOGIC ---
    private val _selectedDayRange = MutableStateFlow<Pair<Long, Long>>(getTodayRange())

    @OptIn(ExperimentalCoroutinesApi::class)
    val ordersForSelectedDate = _selectedDayRange.flatMapLatest { (start, end) ->
        repository.getOrdersByDate(start, end)
    }

    val selectedDateIncome: StateFlow<Double> = ordersForSelectedDate.map { orders ->
        orders.sumOf { it.totalPrice }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // --- ACTIONS ---

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        // 1. Calculate Day Range (Fresh Calendar)
        val dayCal = Calendar.getInstance()
        dayCal.set(year, month, dayOfMonth, 0, 0, 0)
        dayCal.set(Calendar.MILLISECOND, 0)
        val startDay = dayCal.timeInMillis

        dayCal.set(year, month, dayOfMonth, 23, 59, 59)
        dayCal.set(Calendar.MILLISECOND, 999)
        val endDay = dayCal.timeInMillis

        Log.d("AdminViewModel", "Selected Day: $startDay to $endDay")
        _selectedDayRange.value = Pair(startDay, endDay)

        // 2. Calculate Month Range (Fresh Calendar)
        val monthCal = Calendar.getInstance()
        monthCal.set(year, month, 1, 0, 0, 0)
        monthCal.set(Calendar.MILLISECOND, 0)
        val startMonth = monthCal.timeInMillis

        val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        monthCal.set(year, month, maxDay, 23, 59, 59)
        monthCal.set(Calendar.MILLISECOND, 999)
        val endMonth = monthCal.timeInMillis

        _selectedMonthRange.value = Pair(startMonth, endMonth)

        // 3. Update Text
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        _currentMonthName.value = monthFormat.format(monthCal.time)
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return Pair(start, end)
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        Log.d("AdminViewModel", "Today Range: $start to $end")
        return Pair(start, end)
    }

    private fun getCurrentMonthName(): String {
        return SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().time)
    }
}

class AdminViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}