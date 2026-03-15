package com.example.investmentassistant.api

interface FinancialDataService {
    suspend fun getCurrentPrice(ticker: String): Double
    suspend fun getFinancialMetrics(ticker: String): FinancialMetrics
    suspend fun getMacroIndicator(indicatorId: String): Double
}

data class FinancialMetrics(
    val peRatio: Double,
    val salesGrowthYoY: Double,
    val profitMargin: Double
)