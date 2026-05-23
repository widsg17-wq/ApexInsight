package com.example.investmentassistant.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.investmentassistant.R
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.repository.WatchlistRepository
import java.util.concurrent.TimeUnit

class MarketMonitorWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!WatchlistRepository.isMarketOpen()) return Result.success()

        val repo = WatchlistRepository(AppDatabase.getDatabase(applicationContext).watchlistDao())
        val alerts = repo.refreshQuotes()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        alerts.forEach { alert ->
            val direction = if ((alert.quote.dp) > 0) "급등" else "급락"
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("${alert.item.displayName} $direction ${"%.1f".format(alert.quote.dp)}%")
                .setContentText(alert.analysis.take(100))
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.analysis))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            manager.notify(NOTIFICATION_ID_BASE + alert.item.id.toInt(), notification)
        }

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "market_monitor_channel"
        const val NOTIFICATION_ID_BASE = 3000
        const val WORK_NAME = "market_monitor_work"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MarketMonitorWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
