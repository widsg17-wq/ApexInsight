package com.example.investmentassistant.data.repository

import com.example.investmentassistant.api.ApiService
import com.example.investmentassistant.api.RealApiService
import com.example.investmentassistant.model.NewsArticle

interface NewsRepository {
    suspend fun searchNews(query: String, fromDate: String?, toDate: String?): List<NewsArticle>
}

private class NewsRepositoryImpl(
    private val apiService: ApiService = RealApiService(),
) : NewsRepository {
    override suspend fun searchNews(
        query: String,
        fromDate: String?,
        toDate: String?,
    ): List<NewsArticle> = apiService.searchNews(query, fromDate, toDate)
}

fun NewsRepository(): NewsRepository = NewsRepositoryImpl()
