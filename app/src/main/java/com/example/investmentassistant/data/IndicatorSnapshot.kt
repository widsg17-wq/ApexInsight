package com.example.investmentassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "indicator_snapshots")
data class IndicatorSnapshot(
    @PrimaryKey val key: String,
    val value: Float,
    val savedAt: Long = System.currentTimeMillis(),
)
