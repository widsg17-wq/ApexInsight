package com.example.investmentassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.investmentassistant.model.CalendarEvent
import com.example.investmentassistant.model.EventImportance
import com.example.investmentassistant.model.EventType

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val country: String,
    val scheduledAt: Long,
    val previous: String? = null,
    val forecast: String? = null,
    val actual: String? = null,
    val importance: String,
    val ticker: String? = null,
    val earningsTime: String? = null,
    val epsActual: Double? = null,
    val epsEstimate: Double? = null,
    val revenueActual: Long? = null,
    val revenueEstimate: Long? = null,
    val isNotified: Int = 0,
) {
    fun toModel() = CalendarEvent(
        id = id,
        type = EventType.valueOf(type),
        title = title,
        country = country,
        scheduledAt = scheduledAt,
        previous = previous,
        forecast = forecast,
        actual = actual,
        importance = EventImportance.valueOf(importance),
        ticker = ticker,
        earningsTime = earningsTime,
        epsActual = epsActual,
        epsEstimate = epsEstimate,
        revenueActual = revenueActual,
        revenueEstimate = revenueEstimate,
        isNotified = isNotified == 1,
    )
}

fun CalendarEvent.toEntity() = CalendarEventEntity(
    id = id,
    type = type.name,
    title = title,
    country = country,
    scheduledAt = scheduledAt,
    previous = previous,
    forecast = forecast,
    actual = actual,
    importance = importance.name,
    ticker = ticker,
    earningsTime = earningsTime,
    epsActual = epsActual,
    epsEstimate = epsEstimate,
    revenueActual = revenueActual,
    revenueEstimate = revenueEstimate,
    isNotified = if (isNotified) 1 else 0,
)
