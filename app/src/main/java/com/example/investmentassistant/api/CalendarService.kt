package com.example.investmentassistant.api

import retrofit2.http.GET
import retrofit2.http.Query

data class FmpEconomicEvent(
    val event: String = "",
    val date: String = "",
    val country: String = "",
    val actual: String? = null,
    val previous: String? = null,
    val estimate: String? = null,
    val impact: String? = null,
    val unit: String? = null,
    val currency: String? = null,
)

data class FmpEarningsEvent(
    val date: String = "",
    val symbol: String = "",
    val eps: Double? = null,
    val epsEstimated: Double? = null,
    val time: String? = null,
    val revenue: Long? = null,
    val revenueEstimated: Long? = null,
)

interface CalendarService {
    @GET("v3/economic_calendar")
    suspend fun getEconomicCalendar(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("apikey") apiKey: String,
    ): List<FmpEconomicEvent>

    @GET("v3/earning_calendar")
    suspend fun getEarningsCalendar(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("apikey") apiKey: String,
    ): List<FmpEarningsEvent>
}
