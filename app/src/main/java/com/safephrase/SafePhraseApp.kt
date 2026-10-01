package com.safephrase

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.safephrase.core.config.AppConfig
import com.safephrase.core.util.SafeLogger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SafePhraseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initLogging()
        createNotificationChannels()
        SafeLogger.i("SafePhraseApp", "Application started")
    }

    private fun initLogging() {
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
        }
        // In production, plant a crash-reporting tree (e.g. Firebase Crashlytics)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                AppConfig.CHANNEL_ID_MONITORING,
                "SafePhrase Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when SafePhrase protection is active"
                setShowBadge(false)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                AppConfig.CHANNEL_ID_EMERGENCY,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shown when an emergency is triggered"
            }
        )
    }
}
