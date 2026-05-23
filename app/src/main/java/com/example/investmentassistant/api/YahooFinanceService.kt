package com.example.investmentassistant.api

import retrofit2.http.GET
import retrofit2.http.Query

data class YahooSearchQuote(
    val symbol: String = "",
    val shortname: String = "",
    val longname: String = "",
    val quoteType: String = "",
    val exchDisp: String = "",
)

data class YahooSearchResponse(
    val quotes: List<YahooSearchQuote> = emptyList(),
)

interface YahooFinanceService {
    @GET("v1/finance/search")
    suspend fun searchSymbols(
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 15,
        @Query("newsCount") newsCount: Int = 0,
    ): YahooSearchResponse
}
