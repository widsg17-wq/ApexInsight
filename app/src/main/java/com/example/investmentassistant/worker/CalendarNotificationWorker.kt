package com.example.investmentassistant.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
            unnotified.forEach { sendNotification(it) }
            repo.markAsNotified(unnotified.map { it.id })
        }

        return Result.success()
    }

    private fun sendNotification(event: CalendarEvent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "경제 캘린더 알림",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "주요 경제 지표 및 실적 발표 알림" }
        manager.createNotificationChannel(channel)

        val body = when (event.type) {
            EventType.ECONOMIC -> buildEconomicBody(event)
            EventType.EARNINGS -> buildEarningsBody(event)
        }

        val notificationId = NOTIFICATION_ID_BASE + event.id.hashCode()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📅 ${event.title}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(notificationId, notification)
    }

    private fun buildEconomicBody(event: CalendarEvent): String = buildString {
        event.actual?.let { append("실제: $it") }
        event.forecast?.let { append("  예상: $it") }
        event.previous?.let { append("  이전: $it") }
        if (isEmpty()) append("결과가 발표됐습니다.")
    }

    private fun buildEarningsBody(event: CalendarEvent): String = buildString {
        val ticker = event.ticker ?: ""
        appendLine(ticker)
        if (event.epsActual != null) {
            append("EPS 실제: ${"%.2f".format(event.epsActual)}")
            event.epsEstimate?.let { append("  예상: ${"%.2f".format(it)}") }
            appendLine()
        }
        if (event.revenueActual != null) {
            append("매출 실제: ${formatRevenue(event.revenueActual)}")
            event.revenueEstimate?.let { append("  예상: ${formatRevenue(it)}") }
        }
        if (isEmpty()) append("실적이 발표됐습니다.")
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
