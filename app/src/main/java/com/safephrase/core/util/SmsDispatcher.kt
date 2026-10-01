package com.safephrase.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/**
 * Industry-grade SMS dispatcher.
 * Sends SMS directly via cellular radio — no internet required.
 * Handles API version differences, message splitting, and permission checks.
 */
object SmsDispatcher {

    fun sendEmergencySms(
        context: Context,
        phone: String,
        name: String,
        lat: Double?,
        lng: Double?
    ) {
        // ── 1. Permission guard ──────────────────────────────────────────────
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            SafeLogger.e("SmsDispatcher", "SEND_SMS permission not granted! Cannot send to $name")
            return
        }

        // ── 2. Validate phone number ─────────────────────────────────────────
        val cleanPhone = phone.trim()
        if (cleanPhone.isBlank()) {
            SafeLogger.w("SmsDispatcher", "No phone number for contact '$name'")
            return
        }

        // ── 3. Build message ─────────────────────────────────────────────────
        val locationText = if (lat != null && lng != null) {
            "https://maps.google.com/?q=$lat,$lng"
        } else {
            "Location unavailable"
        }
        val message = "EMERGENCY! I need help now! My location: $locationText - Sent via SafePhrase App"

        // ── 4. Get SmsManager (API-safe) ─────────────────────────────────────
        @Suppress("DEPRECATION")
        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }

        // ── 5. Send (auto-split if > 160 chars) ──────────────────────────────
        try {
            val parts = smsManager.divideMessage(message)
            if (parts.size <= 1) {
                smsManager.sendTextMessage(cleanPhone, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
            }
            SafeLogger.i("SmsDispatcher", "✅ SMS fired to $name at $cleanPhone")
        } catch (e: Exception) {
            SafeLogger.e("SmsDispatcher", "❌ SMS failed to $name ($cleanPhone): ${e.message}", e)
        }
    }
}
