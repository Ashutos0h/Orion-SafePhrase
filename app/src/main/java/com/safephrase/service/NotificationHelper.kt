package com.safephrase.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import com.safephrase.MainActivity
import com.safephrase.core.config.AppConfig
import com.safephrase.domain.model.ServiceStatus

object NotificationHelper {

    fun buildMonitoringNotification(
        context: Context,
        status: ServiceStatus
    ): Notification {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val mainPending = PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, MicrophoneMonitorService::class.java).apply {
            action = MicrophoneMonitorService.ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            context, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, content, icon) = when (status) {
            ServiceStatus.MONITORING ->
                Triple("SafePhrase Active", "Monitoring for emergency phrase...", android.R.drawable.ic_btn_speak_now)
            ServiceStatus.RECOVERING ->
                Triple("SafePhrase — Reconnecting", "Recovering microphone...", android.R.drawable.ic_media_pause)
            ServiceStatus.CALL_ACTIVE ->
                Triple("SafePhrase — Call Active", "Monitoring paused during call.", android.R.drawable.ic_menu_call)
            ServiceStatus.MIC_UNAVAILABLE ->
                Triple("SafePhrase — Mic Unavailable", "Microphone cannot be accessed.", android.R.drawable.ic_delete)
            ServiceStatus.EMERGENCY_TRIGGERED ->
                Triple("⚠ EMERGENCY TRIGGERED", "Alert sent. Stay safe.", android.R.drawable.ic_dialog_alert)
            else ->
                Triple("SafePhrase", "Service running.", android.R.drawable.ic_btn_speak_now)
        }

        return NotificationCompat.Builder(context, AppConfig.CHANNEL_ID_MONITORING)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(icon)
            .setContentIntent(mainPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPending)
            .build()
    }

    fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        else 0
        
    fun fireEmergencyTriggeredNotification(context: Context) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPending = PendingIntent.getActivity(
            context, 2, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AppConfig.CHANNEL_ID_EMERGENCY)
            .setContentTitle("⚠ EMERGENCY TRIGGERED")
            .setContentText("Your SafePhrase was detected! Location sent to trusted contacts.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(mainPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
            
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(AppConfig.NOTIF_ID_EMERGENCY, notification)
    }
}
