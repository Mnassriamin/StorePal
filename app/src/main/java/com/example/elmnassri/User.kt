package com.example.elmnassri

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users_table")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String = "",
    val pin: String = "",
    val role: String = ""
)