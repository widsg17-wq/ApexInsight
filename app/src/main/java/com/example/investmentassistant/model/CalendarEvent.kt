package com.example.investmentassistant.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CalendarEvent(
    val id: String,
    val type: EventType,
    val title: String,
    val country: String,
    val scheduledAt: Long,
    val previous: String? = null,
    val forecast: String? = null,
    val actual: String? = null,
    val importance: EventImportance = EventImportance.MEDIUM,
    val ticker: String? = null,
    val earningsTime: String? = null,
    val epsActual: Double? = null,
    val epsEstimate: Double? = null,
    val revenueActual: Long? = null,
    val revenueEstimate: Long? = null,
    val isNotified: Boolean = false,
) {
    val isAnnounced: Boolean
        get() = actual != null || epsActual != null

    val localDate: LocalDate
        get() = Instant.ofEpochMilli(scheduledAt)
            .atZone(ZoneId.of("Asia/Seoul"))
            .toLocalDate()
}

enum class EventType { ECONOMIC, EARNINGS }

enum class EventImportance { HIGH, MEDIUM, LOW }
