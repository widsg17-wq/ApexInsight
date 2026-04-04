package com.example.investmentassistant.api

import com.example.investmentassistant.BuildConfig
import com.example.investmentassistant.model.NewsArticle
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiService {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash", // 방금 수정한 최신 모델명 유지!
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun generateReport(newsArticles: List<NewsArticle>, searchQuery: String): String {
        return withContext(Dispatchers.IO) {
            try {
                if (newsArticles.isEmpty()) {
                    return@withContext "제공된 뉴스 기사가 없어 리포트를 작성할 수 없습니다."
                }

                val articlesText = newsArticles.mapIndexed { index, article ->
                    "[${index + 1}] Title: ${article.title}\nSource: ${article.source}\nLink: ${article.url}"
                }.joinToString("\n\n")

                // ★ 회원님의 요구사항을 반영하여 강력해진 프롬프트입니다 ★
                val prompt = """너는 월스트리트의 수석 투자 애널리스트야. 다음 제공된 뉴스 기사들을 분석하여 '$searchQuery' 테마에 대한 심층 투자 리포트를 작성해줘.

                1. 📊 핵심 동향: 현재 발생하고 있는 주요 이벤트 요약
                2. 💰 경제 및 산업적 파급 효과: 관련 밸류체인 및 거시 경제에 미치는 영향 분석
                3. 🚀 향후 향방 및 투자 시사점: 단기/중장기 시장 전망 및 기회 요인
            
                객관적인 사실을 바탕으로 작성하고, 신뢰성을 위해 리포트 본문 문장 끝에 참조한 기사의 출처를 **반드시 마크다운 하이퍼링크 포맷인 [[1]](기사URL) 형태**로 명시해. 
                (예시: "엔비디아의 주가가 상승했습니다 [[1]](https://example.com).")
            
                [제공된 뉴스 기사들]
                $articlesText
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                response.text ?: "리포트 생성에 실패했습니다."
            } catch (e: Exception) {
                e.printStackTrace()
                "리포트 생성 중 오류가 발생했습니다: ${e.localizedMessage}"
            }
        }
    }
}