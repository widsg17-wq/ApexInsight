package com.example.investmentassistant.api

import com.example.investmentassistant.BuildConfig
import com.example.investmentassistant.model.NewsArticle
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiService {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
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

                val prompt = """
                    너는 전문 투자 애널리스트야. 다음 제공된 뉴스 기사들을 바탕으로 '$searchQuery'에 대한 종합적인 리포트를 작성해줘.
                    사실에 기반해야 하며, 리포트 하단이나 문장 끝에 출처(Source 링크)를 [1], [2] 형태로 반드시 명시해줘.

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
