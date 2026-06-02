package com.example.investmentassistant.fake

import com.example.investmentassistant.data.repository.NewsRepository
import com.example.investmentassistant.model.NewsArticle

class FakeNewsRepository : NewsRepository {

    var shouldThrow = false
    var throwMessage = "뉴스 API 오류"
    var articles: List<NewsArticle> = defaultArticles()

    override suspend fun searchNews(
        query: String,
        fromDate: String?,
        toDate: String?,
    ): List<NewsArticle> {
        if (shouldThrow) throw RuntimeException(throwMessage)
        return articles
    }

    companion object {
        fun defaultArticles() = listOf(
            NewsArticle(
                title = "테스트 뉴스 1",
                source = "Test Source",
                publishedAt = "2024-01-01T00:00:00Z",
                urlToImage = null,
                url = "https://example.com/1",
            ),
            NewsArticle(
                title = "테스트 뉴스 2",
                source = "Test Source 2",
                publishedAt = "2024-01-02T00:00:00Z",
                urlToImage = null,
                url = "https://example.com/2",
            ),
        )
    }
}
