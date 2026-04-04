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
                val prompt = """
                    너는 월스트리트의 수석 투자 애널리스트야. 다음 제공된 뉴스 기사들을 분석하여 '$searchQuery' 테마에 대한 심층 투자 리포트를 작성해줘.

                    단순한 기사 요약을 넘어서, 아래의 세 가지 핵심 섹션을 반드시 포함하여 논리적으로 전개해:
                    1. 📊 핵심 동향: 현재 발생하고 있는 주요 이벤트 요약
                    2. 💰 경제 및 산업적 파급 효과: 이 이슈가 관련 밸류체인, 경쟁사, 그리고 거시 경제에 미치는 구체적인 영향 분석
                    3. 🚀 향후 향방 및 투자 시사점: 현재 상황을 바탕으로 한 단기/중장기 시장 전망과 투자자가 주목해야 할 리스크 및 기회 요인

                    객관적인 사실을 바탕으로 전문적이고 날카로운 통찰력을 보여줘.
                    또한, 신뢰성을 위해 리포트 본문 문장 끝이나 하단에 참조한 기사의 출처(Source 링크)를 [1], [2] 형태로 반드시 명시해.

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