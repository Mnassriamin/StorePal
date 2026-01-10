package com.example.elmnassri

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    // Used for Login: Check if a user exists with this PIN
    @Query("SELECT * FROM users_table WHERE pin = :pin LIMIT 1")
    suspend fun getUserByPin(pin: String): User?

    // Used for your Admin Dashboard: List all workers
    @Query("SELECT * FROM users_table")
    fun getAllUsers(): Flow<List<User>>

    // Helper to see if DB is empty (to create the first Admin account automatically)
    @Query("SELECT COUNT(*) FROM users_table")
    suspend fun getUserCount(): Int
}