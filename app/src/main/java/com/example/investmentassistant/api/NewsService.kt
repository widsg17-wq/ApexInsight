package com.example.investmentassistant.api

interface NewsService {
    suspend fun getNewsByKeyword(keyword: String): List<Article>
    suspend fun getDailyBriefing(assets: List<String>): List<Article>
}

data class Article(
    val title: String,
    val source: String,
    val summary: String,
    val url: String
)