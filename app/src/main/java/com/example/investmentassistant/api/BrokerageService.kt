package com.example.investmentassistant.api

interface BrokerageService {
    suspend fun authenticate(): Boolean
    suspend fun getPortfolio(): Portfolio
    suspend fun executeTrade(ticker: String, shares: Int, action: TradeAction): Boolean
}

data class Portfolio(
    val holdings: List<Holding>,
    val totalValue: Double,
    val cashBalance: Double
)

data class Holding(
    val ticker: String,
    val shares: Int,
    val averagePrice: Double,
    val currentPrice: Double
)

enum class TradeAction {
    BUY, SELL
}