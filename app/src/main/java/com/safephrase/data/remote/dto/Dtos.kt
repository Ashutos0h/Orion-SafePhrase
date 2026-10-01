package com.safephrase.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EmergencyAlertRequest(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("trigger_type") val triggerType: String,
    @SerializedName("keyword_confidence") val keywordConfidence: Float,
    @SerializedName("verification_confidence") val verificationConfidence: Float,
    @SerializedName("phrase_detected") val phraseDetected: String,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("location_accuracy") val locationAccuracy: Float?,
    @SerializedName("location_source") val locationSource: String?
)

data class EmergencyAlertResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("event_id") val eventId: String
)

data class ContactsResponse(
    @SerializedName("contacts") val contacts: List<ContactDto>
)

data class ContactDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("email") val email: String,
    @SerializedName("notification_token") val notificationToken: String
)

data class ApiError(
    @SerializedName("detail") val detail: String
)
