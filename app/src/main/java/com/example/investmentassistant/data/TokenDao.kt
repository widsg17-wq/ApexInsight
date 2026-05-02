package com.example.investmentassistant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TokenDao {
    // 특정 날짜의 기록 가져오기
    @Query("SELECT * FROM token_records WHERE date = :date")
    suspend fun getTokensByDate(date: String): TokenRecord?

    // 기록 저장하기 (이미 같은 날짜가 있으면 덮어쓰기)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokenRecord(record: TokenRecord)

    // 최근 30일치 기록을 최신순으로 가져오기 (테이블 팝업용)
    @Query("SELECT * FROM token_records ORDER BY date DESC LIMIT 30")
    fun getAllTokenRecords(): Flow<List<TokenRecord>>
}