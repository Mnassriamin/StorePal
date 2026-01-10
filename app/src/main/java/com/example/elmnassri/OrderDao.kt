package com.example.elmnassri

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Query("SELECT * FROM orders_table ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    // NEW: Fetch orders within a specific time range
    @Query("SELECT * FROM orders_table WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getOrdersByDateRange(startDate: Long, endDate: Long): Flow<List<Order>>

    @Query("SELECT COUNT(*) FROM orders_table WHERE timestamp = :timestamp")
    suspend fun getOrderCountByTimestamp(timestamp: Long): Int

}