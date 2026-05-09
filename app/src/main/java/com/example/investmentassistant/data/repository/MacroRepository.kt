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

interface MacroRepository {
    suspend fun fetchAllIndicators(range: TimeRange): MacroIndicators
}

private class MacroRepositoryImpl(
    private val fredApi: FredApiService = RealFredApiService(),
    private val financeApi: FinanceApiService = RealFinanceApiService(),
    private val fngApi: FngApiService = RealFngApiService(),
) : MacroRepository {

    override suspend fun fetchAllIndicators(range: TimeRange): MacroIndicators {
        val fLimit = range.fredLimit
        val yInterval = range.yahooInterval
        val yRange = range.yahooRange

        val data10y = fredApi.getMacroData("DGS10", fLimit)
        val data2y = fredApi.getMacroData("DGS2", fLimit)
        val fedData = fredApi.getMacroData("WALCL", fLimit)
        val m2Raw = fredApi.getMacroData("M2SL", fLimit)
        val realRateRaw = fredApi.getMacroData("DFII10", fLimit)
        val hySpreadRaw = fredApi.getMacroData("BAMLH0A0HYM2", fLimit)
        val fng = fngApi.getFearGreedData(fLimit)

        val sp500 = financeApi.getIndexData("^GSPC", yInterval, yRange)
        val nasdaq = financeApi.getIndexData("^IXIC", yInterval, yRange)
        val dollarIndex = financeApi.getIndexData("DX-Y.NYB", yInterval, yRange)
        val vix = financeApi.getIndexData("^VIX", yInterval, yRange)
        val btc = financeApi.getIndexData("BTC-USD", yInterval, yRange)
        val gold = financeApi.getIndexData("GC=F", yInterval, yRange)
        val wti = financeApi.getIndexData("CL=F", yInterval, yRange)
        val copper = financeApi.getIndexData("HG=F", yInterval, yRange)
        val kospi = financeApi.getIndexData("^KS11", yInterval, yRange)
        val usdkrw = financeApi.getIndexData("KRW=X", yInterval, yRange)

        return MacroIndicators(
            us10y = data10y.withPercentSuffix(),
            us2y = data2y.withPercentSuffix(),
            fedBalance = fedData.toTrillions(divisor = 1_000_000f),
            m2 = m2Raw.toTrillions(divisor = 1_000f),
            realRate = realRateRaw.withPercentSuffix(),
            hySpread = hySpreadRaw.withPercentSuffix(),
            sp500 = sp500,
            nasdaq = nasdaq,
            dollarIndex = dollarIndex,
            vix = vix,
            fearGreed = fng,
            btc = btc,
            gold = gold,
            wti = wti,
            copper = copper,
            kospi = kospi,
            usdkrw = usdkrw,
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
