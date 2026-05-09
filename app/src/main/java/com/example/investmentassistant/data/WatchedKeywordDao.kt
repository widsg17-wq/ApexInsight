package com.example.investmentassistant.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedKeywordDao {
    @Query("SELECT * FROM watched_keywords ORDER BY keyword ASC")
    fun getAllKeywords(): Flow<List<WatchedKeyword>>

    @Query("SELECT * FROM watched_keywords")
    suspend fun getAllKeywordsOnce(): List<WatchedKeyword>

    @Insert
    suspend fun insertKeyword(keyword: WatchedKeyword)

    @Delete
    suspend fun deleteKeyword(keyword: WatchedKeyword)

    @Update
    suspend fun updateKeyword(keyword: WatchedKeyword)
}
