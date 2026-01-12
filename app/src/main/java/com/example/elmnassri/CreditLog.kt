package com.example.elmnassri

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_logs_table")
data class CreditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerId: Int = 0, // Added = 0
    val amount: Double = 0.0, // Added = 0.0
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "", // Added = ""
    val orderId: Long? = null
)