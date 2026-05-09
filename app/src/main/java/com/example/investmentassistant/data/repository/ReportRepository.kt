package com.example.investmentassistant.data.repository

import android.content.Context
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.SavedReport
import com.example.investmentassistant.data.TokenRecord
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface ReportRepository {
    fun getAllReports(): Flow<List<SavedReport>>
    suspend fun saveReport(type: String, title: String, content: String)
    suspend fun isReportExists(content: String): Boolean
    suspend fun deleteReport(report: SavedReport)
    fun getTokenRecords(): Flow<List<TokenRecord>>
    suspend fun addTokens(usedTokens: Int)
    suspend fun getLatestAutoReportByKeyword(keyword: String): SavedReport?
}

private class ReportRepositoryImpl(context: Context) : ReportRepository {
    private val db = AppDatabase.getDatabase(context)

    override fun getAllReports(): Flow<List<SavedReport>> = db.reportDao().getAllReports()

    override suspend fun saveReport(type: String, title: String, content: String) {
        db.reportDao().insertReport(SavedReport(type = type, title = title, content = content))
    }

    override suspend fun isReportExists(content: String): Boolean =
        db.reportDao().isReportExists(content)

    override suspend fun deleteReport(report: SavedReport) =
        db.reportDao().deleteReport(report)

    override fun getTokenRecords(): Flow<List<TokenRecord>> =
        db.tokenDao().getAllTokenRecords()

    override suspend fun getLatestAutoReportByKeyword(keyword: String): SavedReport? =
        db.reportDao().getLatestAutoReportByKeyword(keyword)

    override suspend fun addTokens(usedTokens: Int) {
        if (usedTokens <= 0) return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
        val existing = db.tokenDao().getTokensByDate(today)
        db.tokenDao().insertTokenRecord(
            TokenRecord(date = today, totalTokens = (existing?.totalTokens ?: 0) + usedTokens)
        )
    }
}

fun ReportRepository(context: Context): ReportRepository = ReportRepositoryImpl(context)
