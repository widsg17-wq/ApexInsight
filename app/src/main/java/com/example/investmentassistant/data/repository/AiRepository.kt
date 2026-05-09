package com.example.investmentassistant.data.repository

import com.example.investmentassistant.BuildConfig
import com.example.investmentassistant.model.MacroIndicators
import com.example.investmentassistant.model.NewsArticle
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiResult(val text: String, val tokenCount: Int, val tokenUsageStr: String)

interface AiRepository {
    suspend fun generateNewsReport(articles: List<NewsArticle>, searchQuery: String): AiResult
    suspend fun generateMacroInsight(indicators: MacroIndicators): AiResult
    // 직전 리포트와 신규 리포트를 비교해 급변 여부 감지. 급변 시 변화 요약 반환, 없으면 null
    suspend fun detectSignificantChange(previousReport: String, newReport: String, keyword: String): String?
    // 현재 지표를 분석해 투자 포인트 반환. 신호 없으면 null
    suspend fun detectInvestmentOpportunity(indicators: MacroIndicators): String?
}

private class AiRepositoryImpl : AiRepository {
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
    )

    override suspend fun generateNewsReport(
        articles: List<NewsArticle>,
        searchQuery: String,
    ): AiResult {
        if (articles.isEmpty()) return AiResult("제공된 뉴스 기사가 없어 리포트를 작성할 수 없습니다.", 0, "토큰 사용량: 0")

        val articlesText = articles.mapIndexed { i, a ->
            "[${i + 1}] Title: ${a.title}\nSource: ${a.source}\nLink: ${a.url}"
        }.joinToString("\n\n")

        val prompt = """
            너는 월스트리트의 수석 투자 애널리스트야. 다음 제공된 뉴스 기사들을 분석하여 '$searchQuery' 테마에 대한 심층 투자 리포트를 작성해줘.

            1. 📊 핵심 동향: 현재 발생하고 있는 주요 이벤트 요약
            2. 💰 경제 및 산업적 파급 효과: 관련 밸류체인 및 거시 경제에 미치는 영향 분석
            3. 🚀 향후 향방 및 투자 시사점: 단기/중장기 시장 전망 및 기회 요인

            객관적인 사실을 바탕으로 작성하고, 신뢰성을 위해 리포트 본문 문장 끝에 참조한 기사의 출처를 반드시 마크다운 하이퍼링크 포맷인 [[1]](기사URL) 형태로 명시해.

            [제공된 뉴스 기사들]
            $articlesText
        """.trimIndent()

        return call(prompt)
    }

    override suspend fun generateMacroInsight(indicators: MacroIndicators): AiResult {
        val prompt = """
            당신은 월스트리트의 수석 이코노미스트이자 매크로 투자 전문가입니다.
            다음은 현재 시장의 주요 거시경제 지표 실시간 데이터입니다.

            [금리 및 유동성]
            미 국채 10년물: ${indicators.us10y.latestValue}
            미 국채 2년물: ${indicators.us2y.latestValue}
            10Y-2Y 스프레드: ${indicators.us10y.latestValue} - ${indicators.us2y.latestValue}
            연준 자산(유동성): ${indicators.fedBalance.latestValue}
            실질 금리: ${indicators.realRate.latestValue}

            [주식 및 자산]
            S&P 500: ${indicators.sp500.latestValue}
            나스닥: ${indicators.nasdaq.latestValue}
            비트코인: ${"$"}${indicators.btc.latestValue}
            금(Gold): ${"$"}${indicators.gold.latestValue}

            [원자재 및 환율]
            WTI 원유: ${"$"}${indicators.wti.latestValue}
            달러 인덱스: ${indicators.dollarIndex.latestValue}
            원/달러 환율: ₩${indicators.usdkrw.latestValue}
            코스피: ${indicators.kospi.latestValue}

            [시장 심리 및 리스크]
            VIX(공포지수): ${indicators.vix.latestValue}
            하이일드 스프레드: ${indicators.hySpread.latestValue}
            Fear & Greed: ${indicators.fearGreed.latestValue}

            위 데이터를 바탕으로 한국의 개인 투자자를 위해 다음 3가지를 분석해주세요.
            1. 📊 현재 글로벌 매크로 상황 요약 (3줄 이내)
            2. 🎯 자산 시장 단기 방향성 전망
            3. ⚠️ 현재 가장 주의해야 할 핵심 리스크 요인
        """.trimIndent()

        return call(prompt)
    }

    override suspend fun detectInvestmentOpportunity(indicators: MacroIndicators): String? =
        withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    당신은 월스트리트의 수석 투자 전략가입니다. 아래 현재 시장 지표를 분석하여 단기적으로 주목할 만한 투자 포인트(매수/매도 기회)가 있는지 판단해주세요.

                    [현재 주요 지표]
                    S&P 500: ${indicators.sp500.latestValue}
                    나스닥: ${indicators.nasdaq.latestValue}
                    VIX 공포지수: ${indicators.vix.latestValue}
                    Fear & Greed: ${indicators.fearGreed.latestValue}
                    미 국채 10년물: ${indicators.us10y.latestValue}
                    달러 인덱스: ${indicators.dollarIndex.latestValue}
                    금(Gold): ${"$"}${indicators.gold.latestValue}
                    비트코인: ${"$"}${indicators.btc.latestValue}
                    WTI 원유: ${"$"}${indicators.wti.latestValue}
                    원/달러 환율: ₩${indicators.usdkrw.latestValue}
                    코스피: ${indicators.kospi.latestValue}

                    투자 포인트가 있다면 "🎯 [자산]: [간단한 이유]" 형태로 1~3개만 답하세요.
                    특별한 투자 포인트가 없다면 정확히 "NO_SIGNAL"이라고만 답하세요.
                """.trimIndent()

                val response = model.generateContent(prompt)
                val result = response.text?.trim() ?: return@withContext null
                if (result == "NO_SIGNAL") null else result
            } catch (e: Exception) {
                null
            }
        }

    override suspend fun detectSignificantChange(
        previousReport: String,
        newReport: String,
        keyword: String,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                두 투자 리포트를 비교하여 '$keyword' 관련 투자자 관점에서 중요한 변화가 있는지 판단해줘.
                중요한 변화(시장 방향성 전환, 새로운 리스크 등장, 투자 심리 급변 등)가 있다면 한국어로 1-2문장으로 핵심만 설명해줘.
                중요한 변화가 없다면 정확히 "NO_CHANGE"라고만 답해줘.

                [이전 리포트]
                ${previousReport.take(2000)}

                [신규 리포트]
                ${newReport.take(2000)}
            """.trimIndent()

            val response = model.generateContent(prompt)
            val result = response.text?.trim() ?: return@withContext null
            if (result == "NO_CHANGE") null else result
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun call(prompt: String): AiResult = withContext(Dispatchers.IO) {
        try {
            val response = model.generateContent(prompt)
            val usage = response.usageMetadata
            val tokenCount = usage?.totalTokenCount ?: 0
            val tokenStr = if (usage != null) {
                "입력: ${usage.promptTokenCount} | 출력: ${usage.candidatesTokenCount} | 총합: ${usage.totalTokenCount} tokens"
            } else "토큰 사용량: 확인 불가"

            AiResult(
                text = response.text ?: "분석 결과를 받아오지 못했습니다.",
                tokenCount = tokenCount,
                tokenUsageStr = tokenStr,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            AiResult("AI 분석 중 오류가 발생했습니다: ${e.localizedMessage}", 0, "토큰 사용량: 오류")
        }
    }
}

fun AiRepository(): AiRepository = AiRepositoryImpl()
