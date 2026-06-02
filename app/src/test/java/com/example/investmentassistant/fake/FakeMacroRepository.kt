package com.example.investmentassistant.fake

import com.example.investmentassistant.data.repository.MacroRepository
import com.example.investmentassistant.model.MacroData
import com.example.investmentassistant.model.MacroIndicators
import com.example.investmentassistant.model.TimeRange

class FakeMacroRepository : MacroRepository {

    var shouldThrow = false
    var throwMessage = "네트워크 오류"
    var indicators = defaultIndicators()

    override suspend fun fetchAllIndicators(range: TimeRange): MacroIndicators {
        if (shouldThrow) throw RuntimeException(throwMessage)
        return indicators
    }

    companion object {
        fun defaultIndicators() = MacroIndicators(
            us10y = MacroData("4.50%", listOf(4.3f, 4.4f, 4.5f)),
            us2y = MacroData("4.80%", listOf(4.6f, 4.7f, 4.8f)),
            fedBalance = MacroData("7.50T", listOf(7.4f, 7.45f, 7.5f)),
            m2 = MacroData("21.00T", listOf(20.8f, 20.9f, 21.0f)),
            realRate = MacroData("2.10%", listOf(2.0f, 2.05f, 2.1f)),
            hySpread = MacroData("3.50%", listOf(3.3f, 3.4f, 3.5f)),
            sp500 = MacroData("5100", listOf(5000f, 5050f, 5100f)),
            nasdaq = MacroData("16000", listOf(15800f, 15900f, 16000f)),
            dollarIndex = MacroData("104.5", listOf(104f, 104.2f, 104.5f)),
            vix = MacroData("18.5", listOf(17f, 18f, 18.5f)),
            fearGreed = MacroData("55", listOf(50f, 52f, 55f)),
            btc = MacroData("65000", listOf(63000f, 64000f, 65000f)),
            gold = MacroData("2300", listOf(2250f, 2280f, 2300f)),
            wti = MacroData("82.5", listOf(80f, 81f, 82.5f)),
            copper = MacroData("4.5", listOf(4.3f, 4.4f, 4.5f)),
            kospi = MacroData("2650", listOf(2600f, 2625f, 2650f)),
            usdkrw = MacroData("1350", listOf(1340f, 1345f, 1350f)),
        )
    }
}
