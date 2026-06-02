package com.example.investmentassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.investmentassistant.worker.AutoReportWorker
import com.example.investmentassistant.worker.CalendarNotificationWorker
import com.example.investmentassistant.worker.IndicatorAlertWorker
import com.example.investmentassistant.worker.MarketMonitorWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class InvestmentApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        MarketMonitorWorker.schedule(this)
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
