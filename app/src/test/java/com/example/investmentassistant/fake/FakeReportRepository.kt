package com.example.investmentassistant.fake

import com.example.investmentassistant.data.SavedReport
import com.example.investmentassistant.data.TokenRecord
import com.example.investmentassistant.data.repository.ReportRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeReportRepository : ReportRepository {

    private val _reports = MutableStateFlow<List<SavedReport>>(emptyList())
    private val _tokenRecords = MutableStateFlow<List<TokenRecord>>(emptyList())

    val savedReports: List<SavedReport> get() = _reports.value
    var totalTokensAdded: Int = 0

    override fun getAllReports(): Flow<List<SavedReport>> = _reports.asStateFlow()

    override fun getTokenRecords(): Flow<List<TokenRecord>> = _tokenRecords.asStateFlow()

    override suspend fun saveReport(type: String, title: String, content: String) {
        val report = SavedReport(
            id = _reports.value.size + 1,
            type = type,
            title = title,
            content = content,
        )
        _reports.value = _reports.value + report
    }

    override suspend fun deleteReport(report: SavedReport) {
        _reports.value = _reports.value.filter { it.id != report.id }
    }

    override suspend fun isReportExists(content: String): Boolean =
        _reports.value.any { it.content == content }

    override suspend fun addTokens(usedTokens: Int) {
        totalTokensAdded += usedTokens
    }

    override suspend fun getLatestAutoReportByKeyword(keyword: String): SavedReport? =
        _reports.value.lastOrNull { it.title == keyword }
}
