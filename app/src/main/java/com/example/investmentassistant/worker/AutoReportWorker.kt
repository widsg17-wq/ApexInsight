package com.example.investmentassistant.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.investmentassistant.R
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.repository.AiRepository
import com.example.investmentassistant.data.repository.NewsRepository
import com.example.investmentassistant.data.repository.ReportRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutoReportWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val keywords = db.watchedKeywordDao().getAllKeywordsOnce()
        if (keywords.isEmpty()) return Result.success()

        val now = System.currentTimeMillis()
        val newsRepo = NewsRepository()
        val aiRepo = AiRepository()
        val reportRepo = ReportRepository(context)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val generatedKeywords = mutableListOf<String>()

        keywords.forEach { kw ->
            val intervalMs = kw.intervalHours * 60 * 60 * 1000L
            if (now - kw.lastRunAt < intervalMs) return@forEach

            try {
                val fromDate = if (kw.lastRunAt > 0L) {
                    dateFormat.format(Date(kw.lastRunAt))
                } else {
                    dateFormat.format(Date(now - intervalMs))
                }

                val articles = newsRepo.searchNews(kw.keyword, fromDate, null)
                if (articles.isNotEmpty()) {
                    val result = aiRepo.generateNewsReport(articles, kw.keyword)
                    if (result.tokenCount > 0) {
                        reportRepo.saveReport("NEWS", "[자동] ${kw.keyword}", result.text)
                        reportRepo.addTokens(result.tokenCount)
                        generatedKeywords.add(kw.keyword)
                    }
                }
                db.watchedKeywordDao().updateKeyword(kw.copy(lastRunAt = now))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (generatedKeywords.isNotEmpty()) {
            sendNotification(generatedKeywords)
        }

        return Result.success()
    }

    private fun sendNotification(keywords: List<String>) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "자동 리포트",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "키워드 자동 리포트 알림" }
        manager.createNotificationChannel(channel)

        val keywordText = keywords.joinToString(", ")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("새 AI 리포트가 생성됐습니다")
            .setContentText("[$keywordText] 리포트가 보관함에 저장됐습니다.")
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "auto_report_channel"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "auto_report_work"
    }
}
