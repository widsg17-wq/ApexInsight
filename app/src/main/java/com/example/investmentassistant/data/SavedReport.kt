package com.example.investmentassistant.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_reports")
data class SavedReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "NEWS" 또는 "MACRO"
    val title: String, // 리포트 제목 또는 검색어
    val content: String, // AI 리포트 본문
    val savedAt: Long = System.currentTimeMillis() // 저장된 시간
)