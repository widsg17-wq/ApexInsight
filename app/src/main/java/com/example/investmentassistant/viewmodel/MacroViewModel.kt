package com.example.investmentassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.investmentassistant.data.repository.AiRepository
import com.example.investmentassistant.data.repository.MacroRepository
import com.example.investmentassistant.data.repository.ReportRepository
import com.example.investmentassistant.model.MacroIndicators
import com.example.investmentassistant.model.TimeRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MacroViewModel(app: Application) : AndroidViewModel(app) {

    private val macroRepository: MacroRepository = MacroRepository()
    private val aiRepository: AiRepository = AiRepository()
    private val reportRepository: ReportRepository = ReportRepository(app)

    private val _selectedRange = MutableStateFlow(TimeRange.M1)
    val selectedRange: StateFlow<TimeRange> = _selectedRange.asStateFlow()

    private val _indicators = MutableStateFlow(MacroIndicators())
    val indicators: StateFlow<MacroIndicators> = _indicators.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _aiInsight = MutableStateFlow("버튼을 눌러 AI 매크로 분석을 시작하세요.")
    val aiInsight: StateFlow<String> = _aiInsight.asStateFlow()

    private val _isInsightLoading = MutableStateFlow(false)
    val isInsightLoading: StateFlow<Boolean> = _isInsightLoading.asStateFlow()

    private val _tokenUsage = MutableStateFlow("토큰 사용량: -")
    val tokenUsage: StateFlow<String> = _tokenUsage.asStateFlow()

    init {
        fetchMacroData()
    }

    fun updateRange(range: TimeRange) {
        _selectedRange.value = range
        fetchMacroData()
    }

    fun fetchMacroData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _indicators.value = macroRepository.fetchAllIndicators(_selectedRange.value)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateAiInsight() {
        viewModelScope.launch {
            _isInsightLoading.value = true
            _aiInsight.value = "수석 이코노미스트 AI가 13개 지표를 분석 중입니다...\n(최대 10~15초 소요)"
            try {
                val result = aiRepository.generateMacroInsight(_indicators.value)
                _aiInsight.value = result.text
                _tokenUsage.value = result.tokenUsageStr
                reportRepository.addTokens(result.tokenCount)
                if (!reportRepository.isReportExists(result.text)) {
                    reportRepository.saveReport("MACRO", "거시경제 종합 리포트", result.text)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _aiInsight.value = "AI 분석 중 오류가 발생했습니다: ${e.localizedMessage}"
            } finally {
                _isInsightLoading.value = false
            }
        }
    }
}
