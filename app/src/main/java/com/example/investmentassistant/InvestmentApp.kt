package com.example.investmentassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.investmentassistant.worker.AutoReportWorker
import com.example.investmentassistant.worker.CalendarNotificationWorker
import com.example.investmentassistant.worker.IndicatorAlertWorker
import com.example.investmentassistant.worker.MarketMonitorWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class InvestmentApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCoil()
        createNotificationChannels()
        MarketMonitorWorker.schedule(this)
    }

    private fun setupCoil() {
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.20) // 전체 메모리의 20%
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                        .build()
                }
                .crossfade(true)
                .build()
        )
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            NotificationChannel(
                AutoReportWorker.CHANNEL_ID,
                "자동 리포트",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "키워드 자동 리포트 알림" },
            NotificationChannel(
                AutoReportWorker.ALERT_CHANNEL_ID,
                "급변 감지 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "리포트 내용 급변 감지 알림" },
            NotificationChannel(
                CalendarNotificationWorker.CHANNEL_ID,
                "경제 캘린더 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "주요 경제 지표 및 실적 발표 알림" },
            NotificationChannel(
                IndicatorAlertWorker.CHANNEL_ID,
                "지표 급변 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "거시경제 지표 급변 감지 알림" },
            NotificationChannel(
                IndicatorAlertWorker.SIGNAL_CHANNEL_ID,
                "투자 포인트 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "AI 투자 포인트 감지 알림" },
            NotificationChannel(
                MarketMonitorWorker.CHANNEL_ID,
                "종목 급변 알림",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "모니터링 종목 급등·급락 감지 알림" },
        ).forEach { manager.createNotificationChannel(it) }
    }
}
