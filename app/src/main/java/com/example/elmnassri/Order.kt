package com.example.elmnassri

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders_table")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val orderId: Long = 0,

    val timestamp: Long = System.currentTimeMillis(), // Saves the exact time of sale

    val totalPrice: Double = 0.0,
    val workerName: String = "Unknown"
)