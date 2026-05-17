package com.example.investmentassistant.api

import retrofit2.http.GET
import retrofit2.http.Query

data class FinnhubEconomicResponse(
    val economicCalendar: List<FinnhubEconomicEvent>? = null,
)

data class FinnhubEconomicEvent(
    val event: String = "",
    val country: String = "",
    val time: String = "",
    val actual: Double? = null,
    val estimate: Double? = null,
    val prev: Double? = null,
    val impact: String? = null,
    val unit: String? = null,
)

data class FinnhubEarningsResponse(
    val earningsCalendar: List<FinnhubEarningsEvent>? = null,
)

data class FinnhubEarningsEvent(
    val date: String = "",
    val symbol: String = "",
    val epsActual: Double? = null,
    val epsEstimate: Double? = null,
    val hour: String? = null,
    val revenueActual: Long? = null,
    val revenueEstimate: Long? = null,
)

interface CalendarService {
    @GET("calendar/economic")
    suspend fun getEconomicCalendar(
        @Query("token") apiKey: String,
    ): FinnhubEconomicResponse

    @GET("calendar/earnings")
    suspend fun getEarningsCalendar(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") apiKey: String,
    ): FinnhubEarningsResponse
}
