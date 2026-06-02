package com.example.investmentassistant.data.repository

import com.example.investmentassistant.api.FredApiService
import com.example.investmentassistant.api.FinanceApiService
import com.example.investmentassistant.api.FngApiService
import com.example.investmentassistant.api.RealFredApiService
import com.example.investmentassistant.api.RealFinanceApiService
import com.example.investmentassistant.api.RealFngApiService
import com.example.investmentassistant.model.MacroData
import com.example.investmentassistant.model.MacroIndicators
import com.example.investmentassistant.model.TimeRange
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface MacroRepository {
    suspend fun fetchAllIndicators(range: TimeRange): MacroIndicators
}

private class MacroRepositoryImpl(
    private val fredApi: FredApiService = RealFredApiService(),
    private val financeApi: FinanceApiService = RealFinanceApiService(),
    private val fngApi: FngApiService = RealFngApiService(),
) : MacroRepository {

    override suspend fun fetchAllIndicators(range: TimeRange): MacroIndicators = coroutineScope {
        val fLimit = range.fredLimit
        val yInterval = range.yahooInterval
        val yRange = range.yahooRange

        // 17개 API 호출을 모두 동시에 병렬 실행 (순차 → 병렬: 최대 ~17x 속도 개선)
        val d10y    = async { fredApi.getMacroData("DGS10", fLimit) }
        val d2y     = async { fredApi.getMacroData("DGS2", fLimit) }
        val dFed    = async { fredApi.getMacroData("WALCL", fLimit) }
        val dM2     = async { fredApi.getMacroData("M2SL", fLimit) }
        val dReal   = async { fredApi.getMacroData("DFII10", fLimit) }
        val dHy     = async { fredApi.getMacroData("BAMLH0A0HYM2", fLimit) }
        val dFng    = async { fngApi.getFearGreedData(fLimit) }
        val dSp500  = async { financeApi.getIndexData("^GSPC", yInterval, yRange) }
        val dNasdaq = async { financeApi.getIndexData("^IXIC", yInterval, yRange) }
        val dDxy    = async { financeApi.getIndexData("DX-Y.NYB", yInterval, yRange) }
        val dVix    = async { financeApi.getIndexData("^VIX", yInterval, yRange) }
        val dBtc    = async { financeApi.getIndexData("BTC-USD", yInterval, yRange) }
        val dGold   = async { financeApi.getIndexData("GC=F", yInterval, yRange) }
        val dWti    = async { financeApi.getIndexData("CL=F", yInterval, yRange) }
        val dCopper = async { financeApi.getIndexData("HG=F", yInterval, yRange) }
        val dKospi  = async { financeApi.getIndexData("^KS11", yInterval, yRange) }
        val dKrw    = async { financeApi.getIndexData("KRW=X", yInterval, yRange) }

        MacroIndicators(
            us10y      = d10y.await().withPercentSuffix(),
            us2y       = d2y.await().withPercentSuffix(),
            fedBalance = dFed.await().toTrillions(divisor = 1_000_000f),
            m2         = dM2.await().toTrillions(divisor = 1_000f),
            realRate   = dReal.await().withPercentSuffix(),
            hySpread   = dHy.await().withPercentSuffix(),
            fearGreed  = dFng.await(),
            sp500      = dSp500.await(),
            nasdaq     = dNasdaq.await(),
            dollarIndex = dDxy.await(),
            vix        = dVix.await(),
            btc        = dBtc.await(),
            gold       = dGold.await(),
            wti        = dWti.await(),
            copper     = dCopper.await(),
            kospi      = dKospi.await(),
            usdkrw     = dKrw.await(),
        )
    }

    private fun MacroData.withPercentSuffix(): MacroData =
        if (latestValue != "Error") copy(latestValue = "$latestValue%") else this

    private fun MacroData.toTrillions(divisor: Float): MacroData =
        if (latestValue != "Error") {
            val v = latestValue.toFloatOrNull()?.let { it / divisor } ?: 0f
            copy(latestValue = String.format("%.2fT", v))
        } else this
}

fun MacroRepository(): MacroRepository = MacroRepositoryImpl()
