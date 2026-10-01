package com.safephrase.data.local.db.entity

import androidx.room.*

@Entity(tableName = "emergency_events")
data class EmergencyEventEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val triggerType: String,
    val keywordConfidence: Float,
    val verificationConfidence: Float,
    val phraseDetected: String,
    val latitude: Double?,
    val longitude: Double?,
    val locationAccuracy: Float?,
    val locationTimestamp: Long?,
    val locationSource: String?,
    val status: String,        // PENDING, SENDING, SENT, FAILED
    val syncStatus: String,
    val retryCount: Int
)

@Entity(tableName = "trusted_contacts")
data class TrustedContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val notificationToken: String,
    val enabled: Boolean
)

@Entity(tableName = "safe_phrase")
data class SafePhraseEntity(
    @PrimaryKey val id: String,
    val phrase: String,
    val language: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
