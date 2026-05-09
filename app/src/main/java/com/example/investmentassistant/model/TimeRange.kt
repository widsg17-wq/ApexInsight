package com.example.investmentassistant.model

enum class TimeRange(val label: String, val fredLimit: Int, val yahooRange: String, val yahooInterval: String) {
    W1("1W", 7, "5d", "1h"),
    M1("1M", 30, "1mo", "1d"),
    M3("3M", 90, "3mo", "1d"),
    M6("6M", 180, "6mo", "1d"),
    Y1("1Y", 365, "1y", "1wk"),
}
