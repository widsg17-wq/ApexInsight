package com.example.investmentassistant.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.investmentassistant.MainActivity
import com.example.investmentassistant.R
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.repository.CalendarRepository
import com.example.investmentassistant.model.CalendarEvent
import com.example.investmentassistant.model.EventType

class CalendarNotificationWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = CalendarRepository(AppDatabase.getDatabase(context).calendarEventDao())

        try {
            repo.fetchAndCacheCalendar()
        } catch (_: Exception) {}

        val unnotified = repo.getUnnotifiedAnnouncedEvents()
        if (unnotified.isNotEmpty()) {
            sendGroupedNotification(unnotified)
            repo.markAsNotified(unnotified.map { it.id })
        }

        return Result.success()
    }

    private fun sendGroupedNotification(events: List<CalendarEvent>) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "경제 캘린더 알림",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "주요 경제 지표 및 실적 발표 알림" }
        manager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BASE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_DESTINATION, "calendar")
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val title = if (events.size == 1) {
            "📅 ${events.first().title}"
        } else {
            "📅 경제 지표 발표 (${events.size}건)"
        }

        val body = events.joinToString("\n") { event ->
            when (event.type) {
                EventType.ECONOMIC -> buildEconomicLine(event)
                EventType.EARNINGS -> buildEarningsLine(event)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(NOTIFICATION_ID_BASE, notification)
    }

    private fun buildEconomicLine(event: CalendarEvent): String = buildString {
        append(event.title)
        append(" ▶")
        event.actual?.let { append(" 실제: $it") }
        event.forecast?.let { append("  예상: $it") }
        event.previous?.let { append("  이전: $it") }
        if (endsWith("▶")) append(" 결과 발표됨")
    }

    private fun buildEarningsLine(event: CalendarEvent): String = buildString {
        val ticker = event.ticker?.let { "[$it] " } ?: ""
        append("$ticker${event.title} ▶")
        if (event.epsActual != null) {
            append(" EPS 실제: ${"%.2f".format(event.epsActual)}")
            event.epsEstimate?.let { append("  예상: ${"%.2f".format(it)}") }
        }
        if (event.revenueActual != null) {
            append("  매출: ${formatRevenue(event.revenueActual)}")
            event.revenueEstimate?.let { append("  예상: ${formatRevenue(it)}") }
        }
        if (endsWith("▶")) append(" 실적 발표됨")
    }

    private fun formatRevenue(value: Long): String = when {
        value >= 1_000_000_000_000L -> "${"%.1f".format(value / 1_000_000_000_000.0)}T"
        value >= 1_000_000_000L -> "${"%.1f".format(value / 1_000_000_000.0)}B"
        value >= 1_000_000L -> "${"%.1f".format(value / 1_000_000.0)}M"
        else -> value.toString()
    }

    companion object {
        const val CHANNEL_ID = "calendar_notification_channel"
        const val NOTIFICATION_ID_BASE = 2000
        const val WORK_NAME = "calendar_notification_work"
    }
}
