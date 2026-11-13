package com.samjhoenglish.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.samjhoenglish.MainActivity
import com.samjhoenglish.R
import com.samjhoenglish.SamjhoApplication
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.UiStrings
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * About once an hour, while there is internet: asks the school for new tests, tells the parent
 * with a notification, and hands in tests the child finished offline. Free, no Google account.
 * Does nothing for a phone that isn't connected to a school.
 */
class TestCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as SamjhoApplication).container
        val fresh = runCatching { container.school.checkTests() }.getOrElse { return Result.retry() }
        val language = container.settings.appLanguage.first() ?: AppLanguage.HINGLISH
        TestNotifications.show(applicationContext, fresh, language)
        return Result.success()
    }

    companion object {
        private const val NAME = "school-tests"

        /** Safe to call often: keeps the one hourly check that is already scheduled. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TestCheckWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

/** "📝 New English test" notifications for the parent. Tapping one opens the app. */
object TestNotifications {
    private const val CHANNEL = "school_tests"

    private fun L.inLanguage(language: AppLanguage): String = when (language) {
        AppLanguage.HINDI -> hi
        AppLanguage.HINGLISH -> hinglish
        else -> en
    }

    fun show(context: Context, tests: List<NewTest>, language: AppLanguage) {
        if (tests.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, UiStrings.testNotificationChannel.inLanguage(language), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        tests.forEach { test ->
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(UiStrings.testNotificationTitle.inLanguage(language))
                .setContentText(UiStrings.testNotificationText.inLanguage(language).format(test.studentName, test.title))
                .setStyle(NotificationCompat.BigTextStyle().bigText(UiStrings.testNotificationText.inLanguage(language).format(test.studentName, test.title)))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            runCatching {
                NotificationManagerCompat.from(context).notify("test-${test.childId}-${test.title}".hashCode(), notification)
            }
        }
    }
}
