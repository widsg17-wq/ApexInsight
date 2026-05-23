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

data class YahooChartMeta(
    val regularMarketPrice: Double = 0.0,
    val chartPreviousClose: Double = 0.0,
)

data class YahooChartResult(
    val meta: YahooChartMeta = YahooChartMeta(),
)

data class YahooChartData(
    val result: List<YahooChartResult>? = null,
)

data class YahooChartResponse(
    val chart: YahooChartData = YahooChartData(),
)

interface YahooFinanceService {
    @GET("v1/finance/search")
    suspend fun searchSymbols(
        @Query("q") query: String,
        @Query("quotesCount") quotesCount: Int = 15,
        @Query("newsCount") newsCount: Int = 0,
    ): YahooSearchResponse

    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "1d",
    ): YahooChartResponse
}
