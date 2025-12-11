package dev.yaul.twocha

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import dev.yaul.twocha.crash.CrashReporter

@HiltAndroidApp
class TwochaApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "twocha_vpn_channel"
        const val NOTIFICATION_CHANNEL_NAME = "VPN Status"
    }

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows VPN connection status"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}