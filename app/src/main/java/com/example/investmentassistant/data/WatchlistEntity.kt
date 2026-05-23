package com.example.investmentassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.investmentassistant.model.WatchlistItem

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val displayName: String,
    val exchange: String,
    val threshold: Float = 5.0f,
    val lastPrice: Double? = null,
    val lastChangePercent: Double? = null,
    val lastCheckedAt: Long = 0,
    val lastAlertAt: Long = 0,
    val lastAlertMessage: String? = null,
) {
    fun toModel() = WatchlistItem(
        id = id,
        symbol = symbol,
        displayName = displayName,
        exchange = exchange,
        threshold = threshold,
        lastPrice = lastPrice,
        lastChangePercent = lastChangePercent,
        lastCheckedAt = lastCheckedAt,
        lastAlertAt = lastAlertAt,
        lastAlertMessage = lastAlertMessage,
    )
}

fun WatchlistItem.toEntity() = WatchlistEntity(
    id = id,
    symbol = symbol,
    displayName = displayName,
    exchange = exchange,
    threshold = threshold,
    lastPrice = lastPrice,
    lastChangePercent = lastChangePercent,
    lastCheckedAt = lastCheckedAt,
    lastAlertAt = lastAlertAt,
    lastAlertMessage = lastAlertMessage,
)
