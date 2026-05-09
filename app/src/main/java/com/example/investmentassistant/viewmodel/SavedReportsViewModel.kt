package com.example.investmentassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.investmentassistant.data.SavedReport
import com.example.investmentassistant.data.TokenRecord
import com.example.investmentassistant.data.repository.ReportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedReportsViewModel(app: Application) : AndroidViewModel(app) {

    private val reportRepository: ReportRepository = ReportRepository(app)

    val reports: StateFlow<List<SavedReport>> = reportRepository.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tokenRecords: StateFlow<List<TokenRecord>> = reportRepository.getTokenRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteReport(report: SavedReport) {
        viewModelScope.launch { reportRepository.deleteReport(report) }
    }
}
