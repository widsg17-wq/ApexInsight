package com.example.investmentassistant.api

import android.content.Context
import com.example.investmentassistant.BuildConfig
import com.example.investmentassistant.model.NewsArticle
import com.example.investmentassistant.utils.TokenManager
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ★ AI의 답변 내용과 토큰 영수증을 한 번에 전달하기 위한 포장 박스
data class AiResult(val text: String, val tokenUsageStr: String)

class AiService {
    // 앱 전체에서 AI 모델은 여기서 딱 한 번만 세팅합니다!
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    // 📰 1. 뉴스 리포트 전담 함수
    suspend fun generateReport(context: Context, newsArticles: List<NewsArticle>, searchQuery: String): AiResult {
        return withContext(Dispatchers.IO) {
            try {
                if (newsArticles.isEmpty()) {
                    return@withContext AiResult("제공된 뉴스 기사가 없어 리포트를 작성할 수 없습니다.", "토큰 사용량: 0")
                }

                val articlesText = newsArticles.mapIndexed { index, article ->
                    "[${index + 1}] Title: ${article.title}\nSource: ${article.source}\nLink: ${article.url}"
                }.joinToString("\n\n")

                val prompt = """너는 월스트리트의 수석 투자 애널리스트야. 다음 제공된 뉴스 기사들을 분석하여 '$searchQuery' 테마에 대한 심층 투자 리포트를 작성해줘.

                1. 📊 핵심 동향: 현재 발생하고 있는 주요 이벤트 요약
                2. 💰 경제 및 산업적 파급 효과: 관련 밸류체인 및 거시 경제에 미치는 영향 분석
                3. 🚀 향후 향방 및 투자 시사점: 단기/중장기 시장 전망 및 기회 요인
            
                객관적인 사실을 바탕으로 작성하고, 신뢰성을 위해 리포트 본문 문장 끝에 참조한 기사의 출처를 반드시 마크다운 하이퍼링크 포맷인 [[1]](기사URL) 형태**로 명시해. 
                
                [제공된 뉴스 기사들]
                $articlesText
                """.trimIndent()

                // AI 호출 및 토큰 기록 공통 처리 로직으로 넘김
                processAiRequest(context, prompt)

            } catch (e: Exception) {
                e.printStackTrace()
                AiResult("리포트 생성 중 오류가 발생했습니다: ${e.localizedMessage}", "토큰 사용량: 오류")
            }
        }
    }

    // 📊 2. 매크로 리포트 전담 함수 (뷰모델에서 이사 옴!)
    suspend fun generateMacroInsight(context: Context, prompt: String): AiResult {
        return withContext(Dispatchers.IO) {
            try {
                processAiRequest(context, prompt)
            } catch (e: Exception) {
                e.printStackTrace()
                AiResult("AI 분석 중 오류가 발생했습니다: ${e.localizedMessage}", "토큰 사용량: 오류")
            }
        }
    }

    // ⚙️ (내부용) AI 호출하고 토큰 DB에 저장하는 공통 로직
    private suspend fun processAiRequest(context: Context, prompt: String): AiResult {
        val response = generativeModel.generateContent(prompt)
        val usage = response.usageMetadata

        var tokenStr = "토큰 사용량: 확인 불가"

        if (usage != null) {
            tokenStr = "입력: ${usage.promptTokenCount} | 출력: ${usage.candidatesTokenCount} | 총합: ${usage.totalTokenCount} tokens"
            // ★ DB에 토큰 누적 저장!
            TokenManager.addTokensToDb(context, usage.totalTokenCount)
        }

        return AiResult(
            text = response.text ?: "분석 결과를 받아오지 못했습니다.",
            tokenUsageStr = tokenStr
        )
    }
}