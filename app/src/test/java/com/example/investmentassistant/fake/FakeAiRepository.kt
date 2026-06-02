package com.example.investmentassistant.fake

import com.example.investmentassistant.data.repository.AiRepository
import com.example.investmentassistant.data.repository.AiResult
import com.example.investmentassistant.data.repository.TradeRecommendation
import com.example.investmentassistant.data.repository.TradeSignal
import com.example.investmentassistant.model.MacroIndicators
import com.example.investmentassistant.model.NewsArticle

class FakeAiRepository : AiRepository {

    var shouldThrow = false
    var throwMessage = "AI 오류"
    var newsReportResult = AiResult("테스트 뉴스 리포트", 100, "입력: 50 | 출력: 50 | 총합: 100 tokens")
    var macroInsightResult = AiResult("테스트 매크로 인사이트", 200, "입력: 100 | 출력: 100 | 총합: 200 tokens")
    var changeDetectionResult: String? = null
    var opportunityResult: String? = null

    override suspend fun generateNewsReport(
        articles: List<NewsArticle>,
        searchQuery: String,
    ): AiResult {
        if (shouldThrow) throw RuntimeException(throwMessage)
        return newsReportResult
    }

    override suspend fun generateMacroInsight(indicators: MacroIndicators): AiResult {
        if (shouldThrow) throw RuntimeException(throwMessage)
        return macroInsightResult
    }

    override suspend fun detectSignificantChange(
        previousReport: String,
        newReport: String,
        keyword: String,
    ): String? = changeDetectionResult

    override suspend fun detectInvestmentOpportunity(indicators: MacroIndicators): String? =
        opportunityResult

    override suspend fun generateTradeRecommendation(
        symbol: String,
        name: String,
        currentPrice: Double,
        changePercent: Double,
        recentNewsHeadlines: List<String>,
        macroSummary: String,
    ): TradeRecommendation = TradeRecommendation(
        signal = TradeSignal.HOLD,
        confidence = "중간",
        summary = "테스트 추천",
        reasoning = "테스트 분석 결과입니다.",
        targetPrice = null,
        riskNote = "테스트 리스크",
    )
}
