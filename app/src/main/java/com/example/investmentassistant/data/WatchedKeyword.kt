package com.example.investmentassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_keywords")
data class WatchedKeyword(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val keyword: String,
    val intervalHours: Int = 24,
    val lastRunAt: Long = 0L,
)
