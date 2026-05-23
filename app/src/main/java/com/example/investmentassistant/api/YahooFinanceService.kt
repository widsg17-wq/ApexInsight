package com.example.investmentassistant.api

import retrofit2.http.GET
import retrofit2.http.Path
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

data class YahooQuoteResult(
    val symbol: String = "",
    val regularMarketPrice: Double = 0.0,
    val regularMarketChangePercent: Double = 0.0,
    val regularMarketPreviousClose: Double = 0.0,
    val regularMarketOpen: Double = 0.0,
)

data class YahooQuoteResponse(
    val result: List<YahooQuoteResult> = emptyList(),
)

data class YahooQuoteWrapper(
    val quoteResponse: YahooQuoteResponse = YahooQuoteResponse(),
)

interface YahooFinanceService {
    @GET("v1/finance/search")
    suspend fun searchSymbols(
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 15,
        @Query("newsCount") newsCount: Int = 0,
    ): YahooSearchResponse

    @GET("v7/finance/quote")
    suspend fun getQuote(
        @Query("symbols") symbols: String,
    ): YahooQuoteWrapper
}
