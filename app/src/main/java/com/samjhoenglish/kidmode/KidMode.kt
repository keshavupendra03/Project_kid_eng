package com.samjhoenglish.kidmode

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.samjhoenglish.data.SettingsRepository

/** An app on the phone that a parent can choose to lock. */
data class InstalledApp(val packageName: String, val label: String)

/** Turns Kid Mode on and off, and answers what the phone allows. */
class KidMode(private val app: Application, private val settings: SettingsRepository) {

    suspend fun start(childId: Long) {
        settings.setKidModeChild(childId)
        ContextCompat.startForegroundService(app, Intent(app, KidModeService::class.java))
    }

    suspend fun stop() {
        settings.setKidModeChild(null)
        app.stopService(Intent(app, KidModeService::class.java))
    }

    /** Lets us see which app is in front. */
    fun hasUsageAccess(): Boolean {
        val ops = app.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Lets the locked screen appear over another app. */
    fun canShowOverApps(): Boolean = Settings.canDrawOverlays(app)

    fun ignoresBatteryLimits(): Boolean =
        (app.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(app.packageName)

    /**
     * Apps a parent can lock. The phone's home screen and dialer are left out, so the phone
     * stays usable and calls always work. Settings and the Play Store are locked automatically.
     */
    fun lockableApps(): List<InstalledApp> {
        val pm = app.packageManager
        val neverOffered = alwaysLockedPackages + setOfNotNull(app.packageName, homePackage(pm), dialerPackage())
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName !in neverOffered }
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    private fun homePackage(pm: PackageManager): String? =
        pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName

    private fun dialerPackage(): String? =
        (app.getSystemService(Context.TELECOM_SERVICE) as TelecomManager).defaultDialerPackage

    companion object {
        /** Locked in every Kid Mode, so a child can't change permissions or install games. */
        val alwaysLockedPackages = setOf("com.android.settings", "com.android.vending")
    }
}
