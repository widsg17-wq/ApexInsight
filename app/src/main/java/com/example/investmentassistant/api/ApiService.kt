package com.example.investmentassistant.api

import com.example.investmentassistant.model.NewsArticle
import kotlinx.coroutines.delay

interface ApiService {
    suspend fun searchNews(query: String): List<NewsArticle>
}

class MockApiService : ApiService {
    override suspend fun searchNews(query: String): List<NewsArticle> {
        // Simulate network delay
        delay(1000)

        if (query.isBlank()) return emptyList()

        return listOf(
            NewsArticle(
                title = "Tech stocks surge as AI demand grows ($query)",
                source = "Financial Times",
                publishedAt = "2023-10-27T10:00:00Z",
                urlToImage = "https://picsum.photos/seed/tech/400/200",
                url = "https://example.com/news/1"
            ),
            NewsArticle(
                title = "Federal Reserve signals potential rate cuts next year",
                source = "Bloomberg",
                publishedAt = "2023-10-26T15:30:00Z",
                urlToImage = "https://picsum.photos/seed/fed/400/200",
                url = "https://example.com/news/2"
            ),
            NewsArticle(
                title = "New $query policies announced by government",
                source = "Reuters",
                publishedAt = "2023-10-26T08:15:00Z",
                urlToImage = null,
                url = "https://example.com/news/3"
            )
        )
    }
}
