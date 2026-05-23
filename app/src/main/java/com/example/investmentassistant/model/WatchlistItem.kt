package com.example.investmentassistant.model

data class WatchlistItem(
    val id: Long = 0,
    val symbol: String,
    val displayName: String,
    val exchange: String,
    val threshold: Float = 5.0f,
    val lastPrice: Double? = null,
    val lastChangePercent: Double? = null,
    val lastCheckedAt: Long = 0,
    val lastAlertAt: Long = 0,
    val lastAlertMessage: String? = null,
)
