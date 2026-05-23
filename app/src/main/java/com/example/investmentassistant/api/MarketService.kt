package com.example.investmentassistant.api

import retrofit2.http.GET
import retrofit2.http.Query

data class FinnhubQuote(
    val c: Double = 0.0,   // 현재가
    val d: Double = 0.0,   // 변동
    val dp: Double = 0.0,  // 변동률 (%)
    val h: Double = 0.0,   // 고가
    val l: Double = 0.0,   // 저가
    val o: Double = 0.0,   // 시가
    val pc: Double = 0.0,  // 전일 종가
)

data class FinnhubNewsItem(
    val headline: String = "",
    val summary: String = "",
    val source: String = "",
    val url: String = "",
    val datetime: Long = 0,
)

data class FinnhubSymbolResult(
    val description: String = "",
    val displaySymbol: String = "",
    val symbol: String = "",
    val type: String = "",
)

data class FinnhubSearchResponse(
    val count: Int = 0,
    val result: List<FinnhubSymbolResult> = emptyList(),
)

interface MarketService {
    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") apiKey: String,
    ): FinnhubQuote

    @GET("company-news")
    suspend fun getCompanyNews(
        @Query("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String,
    ): List<FinnhubNewsItem>

    @GET("search")
    suspend fun searchSymbol(
        @Query("q") query: String,
        @Query("token") apiKey: String,
    ): FinnhubSearchResponse
}
