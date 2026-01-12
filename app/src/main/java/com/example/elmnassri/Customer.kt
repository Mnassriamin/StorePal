package com.example.elmnassri

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers_table")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val phoneNumber: String = "",
    val totalDebt: Double = 0.0
)