package com.example.investmentassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.investmentassistant.data.repository.AiRepository
import com.example.investmentassistant.data.repository.NewsRepository
import com.example.investmentassistant.data.repository.ReportRepository
import com.example.investmentassistant.model.DateRange
import com.example.investmentassistant.model.NewsArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed interface NewsUiState {
    data object Idle : NewsUiState
    data object Loading : NewsUiState
    data class Success(val articles: List<NewsArticle>, val report: String) : NewsUiState
    data class Error(val message: String) : NewsUiState
}

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val aiRepository: AiRepository,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDateRange = MutableStateFlow(DateRange.ALL)
    val selectedDateRange: StateFlow<DateRange> = _selectedDateRange.asStateFlow()

    private val _customDays = MutableStateFlow("3")
    val customDays: StateFlow<String> = _customDays.asStateFlow()

    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Idle)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateDateRange(range: DateRange) { _selectedDateRange.value = range }
    fun updateCustomDays(days: String) { if (days.all { it.isDigit() }) _customDays.value = days }

    fun searchNews() {
        val query = _searchQuery.value
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = NewsUiState.Loading

            val today = LocalDate.now()
            val fmt = DateTimeFormatter.ISO_DATE
            val daysInput = _customDays.value.toIntOrNull() ?: 3

            val (fromDate, toDate) = when (_selectedDateRange.value) {
                DateRange.PAST_WEEK -> today.minusWeeks(1).format(fmt) to today.format(fmt)
                DateRange.PAST_MONTH -> today.minusMonths(1).format(fmt) to today.format(fmt)
                DateRange.CUSTOM -> today.minusDays(daysInput.toLong()).format(fmt) to today.format(fmt)
                DateRange.ALL -> null to null
            }

            try {
                val articles = newsRepository.searchNews(query, fromDate, toDate)
                val report = if (articles.isNotEmpty()) {
                    val result = aiRepository.generateNewsReport(articles, query)
                    reportRepository.addTokens(result.tokenCount)
                    if (!reportRepository.isReportExists(result.text)) {
                        reportRepository.saveReport("NEWS", query, result.text)
                    }
                    result.text
                } else ""

                _uiState.value = NewsUiState.Success(articles, report)
            } catch (e: Exception) {
                _uiState.value = NewsUiState.Error("검색 실패: ${e.localizedMessage}")
            }
        }
    }
}
