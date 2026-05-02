package com.example.investmentassistant.viewmodel
import android.content.Context // (상단에 임포트 필요할 수 있습니다)
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.investmentassistant.api.FredApiService
import com.example.investmentassistant.api.MacroData
import com.example.investmentassistant.api.RealFredApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.investmentassistant.api.FinanceApiService
import com.example.investmentassistant.api.RealFinanceApiService
import com.example.investmentassistant.api.FngApiService
import com.example.investmentassistant.api.RealFngApiService
import com.example.investmentassistant.api.AiService

enum class TimeRange(val label: String, val fredLimit: Int, val yahooRange: String, val yahooInterval: String) {
    W1("1W", 7, "5d", "1h"),
    M1("1M", 30, "1mo", "1d"),
    M3("3M", 90, "3mo", "1d"),
    M6("6M", 180, "6mo", "1d"),
    Y1("1Y", 365, "1y", "1wk")
}

class MacroViewModel(
    private val aiService: AiService = AiService(),
    private val fredApiService: FredApiService = RealFredApiService() ,
    private val financeApiService: FinanceApiService = RealFinanceApiService() ,
    private val fngApiService: FngApiService = RealFngApiService()
) : ViewModel() {
    private val _aiInsight = MutableStateFlow("버튼을 눌러 AI 매크로 분석을 시작하세요.")
    val aiInsight: StateFlow<String> = _aiInsight.asStateFlow()

    private val _isInsightLoading = MutableStateFlow(false)
    val isInsightLoading: StateFlow<Boolean> = _isInsightLoading.asStateFlow()
    // ★ 기간 설정 상태 추가 ★
    private val _selectedRange = MutableStateFlow(TimeRange.M1)
    val selectedRange: StateFlow<TimeRange> = _selectedRange.asStateFlow()

    // ... (기존 us10yData 부터 usdkrwData 까지의 변수 선언 부분 그대로 유지) ...
    private val _us10yData = MutableStateFlow(MacroData("-", emptyList()))
    val us10yData: StateFlow<MacroData> = _us10yData.asStateFlow()

    private val _us2yData = MutableStateFlow(MacroData("-", emptyList()))
    val us2yData: StateFlow<MacroData> = _us2yData.asStateFlow()

    private val _fedBalanceData = MutableStateFlow(MacroData("-", emptyList()))
    val fedBalanceData: StateFlow<MacroData> = _fedBalanceData.asStateFlow()

    private val _m2Data = MutableStateFlow(MacroData("-", emptyList()))
    val m2Data: StateFlow<MacroData> = _m2Data.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _realRateData = MutableStateFlow(MacroData("-", emptyList()))
    val realRateData: StateFlow<MacroData> = _realRateData.asStateFlow()

    private val _sp500Data = MutableStateFlow(MacroData("-", emptyList()))
    val sp500Data: StateFlow<MacroData> = _sp500Data.asStateFlow()

    private val _nasdaqData = MutableStateFlow(MacroData("-", emptyList()))
    val nasdaqData: StateFlow<MacroData> = _nasdaqData.asStateFlow()

    private val _vixData = MutableStateFlow(MacroData("-", emptyList()))
    val vixData: StateFlow<MacroData> = _vixData.asStateFlow()

    private val _hySpreadData = MutableStateFlow(MacroData("-", emptyList()))
    val hySpreadData: StateFlow<MacroData> = _hySpreadData.asStateFlow()

    private val _fearGreedData = MutableStateFlow(MacroData("-", emptyList()))
    val fearGreedData: StateFlow<MacroData> = _fearGreedData.asStateFlow()

    private val _btcData = MutableStateFlow(MacroData("-", emptyList()))
    val btcData: StateFlow<MacroData> = _btcData.asStateFlow()

    private val _goldData = MutableStateFlow(MacroData("-", emptyList()))
    val goldData: StateFlow<MacroData> = _goldData.asStateFlow()

    private val _wtiData = MutableStateFlow(MacroData("-", emptyList()))
    val wtiData: StateFlow<MacroData> = _wtiData.asStateFlow()

    private val _copperData = MutableStateFlow(MacroData("-", emptyList()))
    val copperData: StateFlow<MacroData> = _copperData.asStateFlow()

    private val _kospiData = MutableStateFlow(MacroData("-", emptyList()))
    val kospiData: StateFlow<MacroData> = _kospiData.asStateFlow()

    private val _usdkrwData = MutableStateFlow(MacroData("-", emptyList()))
    val usdkrwData: StateFlow<MacroData> = _usdkrwData.asStateFlow()

    private val _dollarIndexData = MutableStateFlow(MacroData("-", emptyList()))
    val dollarIndexData: StateFlow<MacroData> = _dollarIndexData.asStateFlow()
    private val _tokenUsage = MutableStateFlow("토큰 사용량: -")
    val tokenUsage: StateFlow<String> = _tokenUsage.asStateFlow()
    init {
        fetchMacroData()
    }

    // ★ 날짜 버튼을 누르면 호출될 함수 ★
    fun updateRange(range: TimeRange) {
        _selectedRange.value = range
        fetchMacroData() // 새로운 기간으로 데이터 재호출!
    }

    fun fetchMacroData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 현재 선택된 기간(1W, 1M, 1Y 등)을 가져옵니다.
                val currentRange = _selectedRange.value
                val fLimit = currentRange.fredLimit
                val yInterval = currentRange.yahooInterval
                val yRange = currentRange.yahooRange

                // ★ API 호출 시 기간(limit, interval, range)을 파라미터로 던져줍니다! ★
                val data10y = fredApiService.getMacroData("DGS10", fLimit)
                val data2y = fredApiService.getMacroData("DGS2", fLimit)
                val fedData = fredApiService.getMacroData("WALCL", fLimit)
                val m2DataRaw = fredApiService.getMacroData("M2SL", fLimit)
                val realRateRaw = fredApiService.getMacroData("DFII10", fLimit)
                val hySpreadRaw = fredApiService.getMacroData("BAMLH0A0HYM2", fLimit)
                val fng = fngApiService.getFearGreedData(fLimit)

                val sp500 = financeApiService.getIndexData("^GSPC", yInterval, yRange)
                val nasdaq = financeApiService.getIndexData("^IXIC", yInterval, yRange)
                val dollarIndex = financeApiService.getIndexData("DX-Y.NYB", yInterval, yRange)
                val vix = financeApiService.getIndexData("^VIX", yInterval, yRange)
                val btc = financeApiService.getIndexData("BTC-USD", yInterval, yRange)
                val gold = financeApiService.getIndexData("GC=F", yInterval, yRange)
                val wti = financeApiService.getIndexData("CL=F", yInterval, yRange)
                val copper = financeApiService.getIndexData("HG=F", yInterval, yRange)
                val kospi = financeApiService.getIndexData("^KS11", yInterval, yRange)
                val usdkrw = financeApiService.getIndexData("KRW=X", yInterval, yRange)

                // (데이터 가공 및 할당은 기존과 동일)
                _us10yData.value = if (data10y.latestValue != "Error") data10y.copy(latestValue = "${data10y.latestValue}%") else data10y
                _us2yData.value = if (data2y.latestValue != "Error") data2y.copy(latestValue = "${data2y.latestValue}%") else data2y
                _fedBalanceData.value = if (fedData.latestValue != "Error") {
                    val trillionValue = fedData.latestValue.toFloatOrNull()?.let { it / 1_000_000 } ?: 0f
                    fedData.copy(latestValue = String.format("%.2fT", trillionValue))
                } else fedData
                _m2Data.value = if (m2DataRaw.latestValue != "Error") {
                    val trillionValue = m2DataRaw.latestValue.toFloatOrNull()?.let { it / 1_000 } ?: 0f
                    m2DataRaw.copy(latestValue = String.format("%.2fT", trillionValue))
                } else m2DataRaw
                _realRateData.value = if (realRateRaw.latestValue != "Error") realRateRaw.copy(latestValue = "${realRateRaw.latestValue}%") else realRateRaw

                _hySpreadData.value = if (hySpreadRaw.latestValue != "Error") hySpreadRaw.copy(latestValue = "${hySpreadRaw.latestValue}%") else hySpreadRaw
                _fearGreedData.value = fng

                _sp500Data.value = sp500
                _nasdaqData.value = nasdaq
                _dollarIndexData.value = dollarIndex
                _vixData.value = vix
                _btcData.value = btc
                _goldData.value = gold
                _wtiData.value = wti
                _copperData.value = copper
                _kospiData.value = kospi
                _usdkrwData.value = usdkrw

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ★ Gemini AI 분석 실행 함수 ★
    fun generateAiInsight(context: Context) {
        viewModelScope.launch {
            _isInsightLoading.value = true
            _aiInsight.value = "수석 이코노미스트 AI가 13개 지표를 분석 중입니다...\n(최대 10~15초 소요)"

            try {
                // 1. 프롬프트 작성 (데이터는 기존과 동일하게 가져옴)
                val prompt = """
                    당신은 월스트리트의 수석 이코노미스트이자 매크로 투자 전문가입니다.
                    다음은 현재 시장의 주요 거시경제 지표 실시간 데이터입니다.

                    [금리 및 유동성]
                    미 국채 10년물: ${_us10yData.value.latestValue}
                    10Y-2Y 스프레드 추이: ${_us10yData.value.latestValue} - ${_us2yData.value.latestValue}
                    연준 자산(유동성): ${_fedBalanceData.value.latestValue}
                    실질 금리: ${_realRateData.value.latestValue}

                    [주식 및 자산]
                    S&P 500: ${_sp500Data.value.latestValue}
                    나스닥: ${_nasdaqData.value.latestValue}
                    비트코인: $${_btcData.value.latestValue}
                    금(Gold): $${_goldData.value.latestValue}

                    [원자재 및 환율]
                    WTI 원유: $${_wtiData.value.latestValue}
                    달러 인덱스: ${_dollarIndexData.value.latestValue}
                    원/달러 환율: ₩${_usdkrwData.value.latestValue}
                    코스피: ${_kospiData.value.latestValue}

                    [시장 심리 및 리스크]
                    VIX(공포지수): ${_vixData.value.latestValue}
                    하이일드 스프레드: ${_hySpreadData.value.latestValue}
                    Fear & Greed: ${_fearGreedData.value.latestValue}

                    위 데이터를 바탕으로 한국의 개인 투자자를 위해 다음 3가지를 분석해주세요.
                    1. 📊 현재 글로벌 매크로 상황 요약 (3줄 이내)
                    2. 🎯 자산 시장 단기 방향성 전망
                    3. ⚠️ 현재 가장 주의해야 할 핵심 리스크 요인
                """.trimIndent()

                // 2. AiService에 분석 요청 및 결과 수신 (텍스트 + 토큰영수증 세트로 받음)
                val result = aiService.generateMacroInsight(context, prompt)

                // 3. 결과를 화면 상태에 업데이트
                _aiInsight.value = result.text
                _tokenUsage.value = result.tokenUsageStr

            } catch (e: Exception) {
                e.printStackTrace()
                _aiInsight.value = "AI 분석 중 오류가 발생했습니다: ${e.localizedMessage}"
            } finally {
                _isInsightLoading.value = false
            }
        }
    }
}