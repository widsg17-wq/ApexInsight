package com.example.investmentassistant.viewmodel

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
class MacroViewModel(
    private val fredApiService: FredApiService = RealFredApiService() ,
    private val financeApiService: FinanceApiService = RealFinanceApiService() ,
    private val fngApiService: FngApiService = RealFngApiService()
) : ViewModel() {

    // 금리 데이터 상태
    private val _us10yData = MutableStateFlow(MacroData("-", emptyList()))
    val us10yData: StateFlow<MacroData> = _us10yData.asStateFlow()

    private val _us2yData = MutableStateFlow(MacroData("-", emptyList()))
    val us2yData: StateFlow<MacroData> = _us2yData.asStateFlow()

    // ★ 유동성 데이터 상태 추가 ★
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

    // [원자재]
    private val _wtiData = MutableStateFlow(MacroData("-", emptyList()))
    val wtiData: StateFlow<MacroData> = _wtiData.asStateFlow()

    private val _copperData = MutableStateFlow(MacroData("-", emptyList()))
    val copperData: StateFlow<MacroData> = _copperData.asStateFlow()

    // [한국 시장]
    private val _kospiData = MutableStateFlow(MacroData("-", emptyList()))
    val kospiData: StateFlow<MacroData> = _kospiData.asStateFlow()

    private val _usdkrwData = MutableStateFlow(MacroData("-", emptyList()))
    val usdkrwData: StateFlow<MacroData> = _usdkrwData.asStateFlow()

    init {
        fetchMacroData()
    }
    private val _dollarIndexData = MutableStateFlow(MacroData("-", emptyList()))
    val dollarIndexData: StateFlow<MacroData> = _dollarIndexData.asStateFlow()
    fun fetchMacroData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. 금리 데이터 호출
                val data10y = fredApiService.getMacroData("DGS10")
                val data2y = fredApiService.getMacroData("DGS2")

                // 2. 유동성 데이터 호출 (WALCL: 연준 자산, M2SL: M2 통화량)
                val fedData = fredApiService.getMacroData("WALCL")
                val m2DataRaw = fredApiService.getMacroData("M2SL")
                // ★ 2. 실질 금리(DFII10) 데이터 호출
                val realRateRaw = fredApiService.getMacroData("DFII10")
                // 3. 금리 데이터 가공 (%)
                _us10yData.value = if (data10y.latestValue != "Error") {
                    data10y.copy(latestValue = "${data10y.latestValue}%")
                } else data10y

                _us2yData.value = if (data2y.latestValue != "Error") {
                    data2y.copy(latestValue = "${data2y.latestValue}%")
                } else data2y

                // 4. 유동성 데이터 가공 (Trillion 'T' 단위로 변환)
                // FRED의 WALCL은 '백만 달러' 기준이므로 100만을 나눠서 조(T) 단위로 맞춥니다.
                _fedBalanceData.value = if (fedData.latestValue != "Error") {
                    val trillionValue = fedData.latestValue.toFloatOrNull()?.let { it / 1_000_000 } ?: 0f
                    fedData.copy(latestValue = String.format("%.2fT", trillionValue))
                } else fedData

                // FRED의 M2SL은 '십억 달러' 기준이므로 1,000을 나눠서 조(T) 단위로 맞춥니다.
                _m2Data.value = if (m2DataRaw.latestValue != "Error") {
                    val trillionValue = m2DataRaw.latestValue.toFloatOrNull()?.let { it / 1_000 } ?: 0f
                    m2DataRaw.copy(latestValue = String.format("%.2fT", trillionValue))
                } else m2DataRaw
                // ★ 3. 실질 금리 데이터 가공 (%)
                _realRateData.value = if (realRateRaw.latestValue != "Error") {
                    realRateRaw.copy(latestValue = "${realRateRaw.latestValue}%")
                } else realRateRaw
                val sp500 = financeApiService.getIndexData("^GSPC")
                val nasdaq = financeApiService.getIndexData("^IXIC")
                val dollarIndex = financeApiService.getIndexData("DX-Y.NYB")
                _sp500Data.value = sp500
                _nasdaqData.value = nasdaq
                _dollarIndexData.value = dollarIndex
                val vix = financeApiService.getIndexData("^VIX") // 야후에서 VIX 호출
                val hySpreadRaw = fredApiService.getMacroData("BAMLH0A0HYM2") // FRED에서 하이일드 호출_vixData.value = vix
                _vixData.value = vix
                _hySpreadData.value = if (hySpreadRaw.latestValue != "Error") {
                    hySpreadRaw.copy(latestValue = "${hySpreadRaw.latestValue}%")
                } else hySpreadRaw
                val fng = fngApiService.getFearGreedData()
                val btc = financeApiService.getIndexData("BTC-USD") // 비트코인
                val gold = financeApiService.getIndexData("GC=F") // 금 선물
                val wti = financeApiService.getIndexData("CL=F") // WTI 원유 선물
                val copper = financeApiService.getIndexData("HG=F") // 구리 선물
                val kospi = financeApiService.getIndexData("^KS11") // 코스피 지수
                val usdkrw = financeApiService.getIndexData("KRW=X") // 원/달러 환율
                _fearGreedData.value = fng
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
}