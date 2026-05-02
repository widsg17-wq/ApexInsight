package com.example.investmentassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "token_records")
data class TokenRecord(
    @PrimaryKey val date: String, // "2024-05-12" 같은 날짜를 고유 키로 사용
    val totalTokens: Int
)