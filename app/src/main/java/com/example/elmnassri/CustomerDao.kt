package com.example.elmnassri

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("SELECT * FROM customers_table ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers_table WHERE id = :id")
    suspend fun getCustomerById(id: Int): Customer?

    // --- ADD THIS NEW FUNCTION ---
    @Query("SELECT * FROM customers_table WHERE name = :name LIMIT 1")
    suspend fun getCustomerByName(name: String): Customer?

    // LOGS
    @Insert
    suspend fun insertCreditLog(log: CreditLog)

    @Query("SELECT * FROM credit_logs_table WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getLogsForCustomer(customerId: Int): Flow<List<CreditLog>>

    @Query("SELECT * FROM credit_logs_table WHERE customerId = :customerId AND timestamp = :timestamp LIMIT 1")
    suspend fun getCreditLogByDetails(customerId: Int, timestamp: Long): CreditLog?
}