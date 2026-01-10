package com.example.elmnassri

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_items_table")
data class OrderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Added default values (= 0 or = "") to all fields
    val orderId: Long = 0,
    val barcode: String = "",
    val itemName: String = "",
    val priceAtSale: Double = 0.0,
    val quantity: Int = 0
)