package com.hotwheelsgarage.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hotwheelsgarage.HotWheelsApp
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Background drop check. Runs only with internet and when the battery isn't low; skips quiet hours. */
class DropCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as HotWheelsApp
        if (!app.settings.alertsEnabled) return Result.success()
        if (app.settings.isQuietNow(LocalTime.now().hour)) return Result.success()
        DropChecker(app).check()
        return Result.success()
    }
}

/** Daily nudge a few minutes before r/HotWheelsIndia's 6 pm Sales Hour. */
class SalesHourWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as HotWheelsApp
        if (app.settings.alertsEnabled && app.settings.salesHourReminder) {
            Notifications.show(
                app, 6_00, Notifications.CHANNEL_REMINDERS,
                title = "🛍️ Sales Hour starts at 6 pm",
                text = "Collectors post cars at MRP on r/HotWheelsIndia. Check your wishlist!",
                url = "https://www.reddit.com/r/HotWheelsIndia/new/",
            )
        }
        return Result.success()
    }
}

object AlertScheduler {
    private const val DROP_WORK = "drop-check"
    private const val SALES_HOUR_WORK = "sales-hour"

    /** Call on app start and whenever alert settings change. */
    fun apply(context: Context) {
        val app = context.applicationContext as HotWheelsApp
        val work = WorkManager.getInstance(context)
        if (!app.settings.alertsEnabled) {
            work.cancelUniqueWork(DROP_WORK)
            work.cancelUniqueWork(SALES_HOUR_WORK)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        work.enqueueUniquePeriodicWork(
            DROP_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<DropCheckWorker>(app.settings.checkIntervalMinutes.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build(),
        )
        if (app.settings.salesHourReminder) {
            work.enqueueUniquePeriodicWork(
                SALES_HOUR_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<SalesHourWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delayUntil(LocalTime.of(17, 50)).toMinutes(), TimeUnit.MINUTES)
                    .build(),
            )
        } else {
            work.cancelUniqueWork(SALES_HOUR_WORK)
        }
    }

    private fun delayUntil(time: LocalTime): Duration {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}
