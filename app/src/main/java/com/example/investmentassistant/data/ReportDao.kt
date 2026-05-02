package com.example.investmentassistant.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    // 1. 리포트 저장하기
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: SavedReport)

    @Query("SELECT EXISTS(SELECT * FROM saved_reports WHERE content = :content)")
    suspend fun isReportExists(content: String): Boolean
    // 2. 저장된 모든 리포트 가져오기 (최신순)
    @Query("SELECT * FROM saved_reports ORDER BY savedAt DESC")
    fun getAllReports(): Flow<List<SavedReport>>

    // 3. 특정 타입(NEWS/MACRO)만 가져오기
    @Query("SELECT * FROM saved_reports WHERE type = :type ORDER BY savedAt DESC")
    fun getReportsByType(type: String): Flow<List<SavedReport>>

    // 4. 리포트 삭제하기
    @Delete
    suspend fun deleteReport(report: SavedReport)
}