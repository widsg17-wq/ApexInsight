package com.example.investmentassistant.model

data class NewsArticle(
    val title: String,
    val source: String,
    val publishedAt: String,
    val urlToImage: String?,
    val url: String
)
