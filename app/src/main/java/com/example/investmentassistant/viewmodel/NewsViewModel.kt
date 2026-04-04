package com.example.investmentassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.investmentassistant.api.AiService
import com.example.investmentassistant.api.ApiService
import com.example.investmentassistant.api.RealApiService
import com.example.investmentassistant.model.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 기간 선택 옵션 정의
enum class DateRange(val label: String) {
    ALL("전체"),
    PAST_WEEK("최근 1주일"),
    PAST_MONTH("최근 1개월"),
    CUSTOM("직접 설정")
}

class NewsViewModel(
    private val apiService: ApiService = RealApiService(),
    private val aiService: AiService = AiService()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDateRange = MutableStateFlow(DateRange.ALL)
    val selectedDateRange: StateFlow<DateRange> = _selectedDateRange.asStateFlow()

    private val _customDays = MutableStateFlow("3")
    val customDays: StateFlow<String> = _customDays.asStateFlow()

    private val _newsList = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsList: StateFlow<List<NewsArticle>> = _newsList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _generatedReport = MutableStateFlow<String>("")
    val generatedReport: StateFlow<String> = _generatedReport.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateDateRange(range: DateRange) {
        _selectedDateRange.value = range
    }

    fun updateCustomDays(days: String) {
        if (days.all { it.isDigit() }) {
            _customDays.value = days
        }
    }

    fun searchNews() {
        val query = _searchQuery.value
        val range = _selectedDateRange.value
        if (query.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _generatedReport.value = ""

            // 날짜 계산 로직
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ISO_DATE
            var fromDate: String? = null
            var toDate: String? = today.format(formatter)

            val daysInput = _customDays.value.toIntOrNull() ?: 0

            when (range) {
                DateRange.PAST_WEEK -> fromDate = today.minusWeeks(1).format(formatter)
                DateRange.PAST_MONTH -> fromDate = today.minusMonths(1).format(formatter)
                DateRange.CUSTOM -> fromDate = today.minusDays(daysInput.toLong()).format(formatter)
                DateRange.ALL -> { fromDate = null; toDate = null }
            }

            try {
                val results = apiService.searchNews(query, fromDate, toDate)
                _newsList.value = results

                if (results.isNotEmpty()) {
                    val report = aiService.generateReport(results, query)
                    _generatedReport.value = report
                }
            } catch (e: Exception) {
                _newsList.value = emptyList()
                _generatedReport.value = "리포트 생성 실패: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}