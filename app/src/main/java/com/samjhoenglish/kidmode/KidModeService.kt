package com.samjhoenglish.kidmode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.samjhoenglish.MainActivity
import com.samjhoenglish.R
import com.samjhoenglish.SamjhoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs while Kid Mode is on: checks which app is in front and covers it if a parent locked it.
 * If the phone kills this service, Android restarts it and it picks up from the saved settings.
 */
class KidModeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watcher: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification()
        if (watcher == null) watcher = scope.launch { watchApps() }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        Log.i(TAG, "service stopped")
        super.onDestroy()
    }

    private suspend fun watchApps() {
        val settings = (application as SamjhoApplication).container.settings
        if (settings.kidModeChild.first() == null) {
            stopSelf()
            return
        }
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        var checkedUntil = System.currentTimeMillis() - START_LOOKBACK_MS
        var nextHeartbeat = 0L
        Log.i(TAG, "service watching")

        while (scope.isActive) {
            val now = System.currentTimeMillis()
            // Null while the phone is locked; there's nothing to cover then.
            val opened = usage.queryEvents(checkedUntil, now)?.let(::lastOpenedApp)
            checkedUntil = now
            if (opened != null && opened != packageName) {
                val locked = settings.lockedApps.first() + KidMode.alwaysLockedPackages
                if (opened in locked) {
                    Log.i(TAG, "blocked $opened")
                    startActivity(LockedAppActivity.intent(this, opened))
                }
            }
            if (now >= nextHeartbeat) {
                Log.i(TAG, "alive")
                nextHeartbeat = now + HEARTBEAT_MS
            }
            delay(CHECK_EVERY_MS)
        }
    }

    private fun lastOpenedApp(events: UsageEvents): String? {
        val event = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = event.packageName
        }
        return last
    }

    private fun showNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Kid Mode", NotificationManager.IMPORTANCE_LOW))
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Samjho English")
            .setContentText("Kid Mode")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val TAG = "KidMode"
        const val CHANNEL = "kid_mode"
        const val NOTIFICATION_ID = 7
        const val CHECK_EVERY_MS = 500L
        const val START_LOOKBACK_MS = 5_000L
        const val HEARTBEAT_MS = 60_000L
    }
}
