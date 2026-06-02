package com.example.investmentassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.investmentassistant.data.SavedReport
import com.example.investmentassistant.data.TokenRecord
import com.example.investmentassistant.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {

    val reports: StateFlow<List<SavedReport>> = reportRepository.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tokenRecords: StateFlow<List<TokenRecord>> = reportRepository.getTokenRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteReport(report: SavedReport) {
        viewModelScope.launch { reportRepository.deleteReport(report) }
    }
}
