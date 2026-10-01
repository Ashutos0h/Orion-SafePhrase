package com.safephrase.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.safephrase.core.util.SafeLogger

/**
 * Boot receiver — shows user that protection must be restarted manually.
 *
 * Per current Android policy (Android 12+), apps cannot auto-start microphone
 * foreground services on boot from background. We deliberately do NOT attempt
 * to start the service here. Instead, we could show a notification reminding
 * the user to reopen the app and tap "Start Protection".
 */
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.safephrase.MainActivity
import com.safephrase.core.config.AppConfig
import com.safephrase.service.NotificationHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        SafeLogger.i("BootReceiver", "Boot detected — notifying user to restart protection")
        
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AppConfig.CHANNEL_ID_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("SafePhrase Protection Paused")
            .setContentText("Your device restarted. Tap to restart SafePhrase.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2001, notification)
    }
}
