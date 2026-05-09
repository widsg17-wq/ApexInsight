package com.example.investmentassistant.model

data class MacroData(val latestValue: String, val history: List<Float>)

data class MacroIndicators(
    val us10y: MacroData = MacroData("-", emptyList()),
    val us2y: MacroData = MacroData("-", emptyList()),
    val fedBalance: MacroData = MacroData("-", emptyList()),
    val m2: MacroData = MacroData("-", emptyList()),
    val realRate: MacroData = MacroData("-", emptyList()),
    val hySpread: MacroData = MacroData("-", emptyList()),
    val sp500: MacroData = MacroData("-", emptyList()),
    val nasdaq: MacroData = MacroData("-", emptyList()),
    val dollarIndex: MacroData = MacroData("-", emptyList()),
    val vix: MacroData = MacroData("-", emptyList()),
    val fearGreed: MacroData = MacroData("-", emptyList()),
    val btc: MacroData = MacroData("-", emptyList()),
    val gold: MacroData = MacroData("-", emptyList()),
    val wti: MacroData = MacroData("-", emptyList()),
    val copper: MacroData = MacroData("-", emptyList()),
    val kospi: MacroData = MacroData("-", emptyList()),
    val usdkrw: MacroData = MacroData("-", emptyList()),
)