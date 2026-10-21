package com.hotwheelsgarage.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hotwheelsgarage.R

object Notifications {
    const val CHANNEL_DROPS = "drops"
    const val CHANNEL_REMINDERS = "reminders"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DROPS, "Drop & sale alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Store drops and sales that mention cars you want" }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Sales Hour reminder", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Daily reminder before r/HotWheelsIndia's 6 pm Sales Hour" }
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Tapping the notification opens [url] (the post) in the browser or Reddit/Telegram app. */
    fun show(context: Context, id: Int, channel: String, title: String, text: String, url: String) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, id, Intent(Intent.ACTION_VIEW, Uri.parse(url)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
