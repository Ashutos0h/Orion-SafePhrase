package com.safephrase.data.remote.api

import com.safephrase.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface SafePhraseApi {

    @POST("api/v1/emergency/alert")
    suspend fun sendEmergencyAlert(
        @Header("Authorization") token: String,
        @Body request: EmergencyAlertRequest
    ): Response<EmergencyAlertResponse>

    @GET("api/v1/contacts")
    suspend fun getTrustedContacts(
        @Header("Authorization") token: String
    ): Response<ContactsResponse>

    @GET("api/v1/health")
    suspend fun healthCheck(): Response<Unit>
}
