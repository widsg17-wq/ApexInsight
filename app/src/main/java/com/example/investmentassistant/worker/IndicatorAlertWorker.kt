package com.example.investmentassistant.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.investmentassistant.R
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.IndicatorSnapshot
import com.example.investmentassistant.data.repository.AiRepository
import com.example.investmentassistant.data.repository.MacroRepository
import com.example.investmentassistant.model.MacroIndicators
import com.example.investmentassistant.model.TimeRange

class IndicatorAlertWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val prefs = context.getSharedPreferences("alert_prefs", Context.MODE_PRIVATE)
        val macroRepo = MacroRepository()
        val aiRepo = AiRepository()

        val current = try {
            macroRepo.fetchAllIndicators(TimeRange.W1)
        } catch (e: Exception) {
            return Result.retry()
        }

        val currentMap = current.toMap()
        val previous = db.indicatorSnapshotDao().getAll().associateBy { it.key }

        // 지표 급변 체크
        val alerts = mutableListOf<String>()
        currentMap.forEach { (key, currentVal) ->
            val prevSnapshot = previous[key] ?: return@forEach
            val prevVal = prevSnapshot.value
            if (prevVal == 0f) return@forEach

            val changePct = ((currentVal - prevVal) / prevVal) * 100f
            val threshold = THRESHOLDS[key] ?: return@forEach

            if (Math.abs(changePct) >= threshold) {
                val direction = if (changePct > 0) "▲" else "▼"
                val name = DISPLAY_NAMES[key] ?: key
                alerts.add("$name $direction ${"%.1f".format(Math.abs(changePct))}%")
            }
        }

        // 현재값을 스냅샷으로 저장
        db.indicatorSnapshotDao().upsertAll(
            currentMap.map { (key, value) -> IndicatorSnapshot(key = key, value = value) }
        )

        if (alerts.isNotEmpty()) {
            sendNotification(alerts)
        }

        // 투자 포인트 감지 (토글이 켜진 경우)
        if (prefs.getBoolean("investment_alert_enabled", false)) {
            val signal = aiRepo.detectInvestmentOpportunity(current)
            if (signal != null) {
                sendInvestmentSignalNotification(signal)
            }
        }

        return Result.success()
    }

    private fun sendNotification(alerts: List<String>) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "지표 급변 알림",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "거시경제 지표 급변 감지 알림" }
        manager.createNotificationChannel(channel)

        val body = alerts.joinToString("\n")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📊 지표 급변 감지 (${alerts.size}개)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendInvestmentSignalNotification(signal: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            SIGNAL_CHANNEL_ID,
            "투자 포인트 알림",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "AI 투자 포인트 감지 알림" }
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, SIGNAL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎯 투자 포인트 감지")
            .setStyle(NotificationCompat.BigTextStyle().bigText(signal))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(SIGNAL_NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "indicator_alert_channel"
        const val NOTIFICATION_ID = 1003
        const val SIGNAL_CHANNEL_ID = "investment_signal_channel"
        const val SIGNAL_NOTIFICATION_ID = 1004
        const val WORK_NAME = "indicator_alert_work"

        // 각 지표별 알림 임계값 (%)
        val THRESHOLDS = mapOf(
            "vix" to 15f,
            "fearGreed" to 15f,
            "sp500" to 2f,
            "nasdaq" to 2.5f,
            "btc" to 5f,
            "gold" to 3f,
            "wti" to 4f,
            "dollarIndex" to 1.5f,
            "usdkrw" to 1.5f,
            "kospi" to 2f,
            "us10y" to 5f,
            "us2y" to 5f,
        )

        val DISPLAY_NAMES = mapOf(
            "vix" to "VIX 공포지수",
            "fearGreed" to "Fear & Greed",
            "sp500" to "S&P 500",
            "nasdaq" to "나스닥",
            "btc" to "비트코인",
            "gold" to "금(Gold)",
            "wti" to "WTI 원유",
            "dollarIndex" to "달러 인덱스",
            "usdkrw" to "원/달러 환율",
            "kospi" to "코스피",
            "us10y" to "미 국채 10년물",
            "us2y" to "미 국채 2년물",
        )
    }
}

// MacroIndicators → Map<String, Float> 변환 (history 마지막 값 사용)
private fun MacroIndicators.toMap(): Map<String, Float> = buildMap {
    us10y.history.lastOrNull()?.let { put("us10y", it) }
    us2y.history.lastOrNull()?.let { put("us2y", it) }
    vix.history.lastOrNull()?.let { put("vix", it) }
    fearGreed.history.lastOrNull()?.let { put("fearGreed", it) }
    sp500.history.lastOrNull()?.let { put("sp500", it) }
    nasdaq.history.lastOrNull()?.let { put("nasdaq", it) }
    btc.history.lastOrNull()?.let { put("btc", it) }
    gold.history.lastOrNull()?.let { put("gold", it) }
    wti.history.lastOrNull()?.let { put("wti", it) }
    dollarIndex.history.lastOrNull()?.let { put("dollarIndex", it) }
    usdkrw.history.lastOrNull()?.let { put("usdkrw", it) }
    kospi.history.lastOrNull()?.let { put("kospi", it) }
}
