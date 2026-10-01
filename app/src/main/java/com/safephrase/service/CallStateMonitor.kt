package com.safephrase.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.TelephonyManager
import com.safephrase.core.util.SafeLogger

private const val TAG = "CallStateMonitor"

/**
 * Monitors telephony call state changes.
 * Uses BroadcastReceiver instead of deprecated PhoneStateListener to support
 * Android 12+ where the listener approach requires READ_CALL_LOG permission.
 */
class CallStateMonitor(
    private val context: Context,
    private val onCallStarted: () -> Unit,
    private val onCallEnded: () -> Unit
) {
    private var isCallActive = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            SafeLogger.d(TAG, "Phone state: $state")

            when (state) {
                TelephonyManager.EXTRA_STATE_OFFHOOK,
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    if (!isCallActive) {
                        isCallActive = true
                        onCallStarted()
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    if (isCallActive) {
                        isCallActive = false
                        onCallEnded()
                    }
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        context.registerReceiver(receiver, filter)
        SafeLogger.i(TAG, "CallStateMonitor registered")
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) { /* already unregistered */ }
        SafeLogger.i(TAG, "CallStateMonitor unregistered")
    }
}
