package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val query: String,
    val response: String,
    val commandType: String = "CHAT",
    val isVoice: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val actionPayload: String? = null
)
