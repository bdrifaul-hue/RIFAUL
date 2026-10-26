package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "USER", "AI", "SYSTEM"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "en", // "bn", "en", "auto"
    val actionType: String? = null, // "APP_LAUNCH", "APP_UNAVAILABLE", "SYSTEM_INFO"
    val actionTargetName: String? = null,
    val actionTargetPackage: String? = null,
    val actionFallbackUrl: String? = null,
    val actionStatus: String? = null // "VERIFIED_LAUNCHED", "PENDING_CONFIRMATION", "UNAVAILABLE", "FAILED"
)
