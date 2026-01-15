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

    @Query("SELECT * FROM users_table WHERE pin = :pin LIMIT 1")
    suspend fun getUserByPin(pin: String): User?

    @Query("SELECT COUNT(*) FROM users_table")
    suspend fun getUserCount(): Int

    // NEW: Get all users to display in the list
    @Query("SELECT * FROM users_table ORDER BY role ASC, name ASC")
    fun getAllUsers(): Flow<List<User>>
}