package com.example.investmentassistant.data.repository

import android.content.Context
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.WatchedKeyword
import kotlinx.coroutines.flow.Flow

interface WatchedKeywordRepository {
    fun getAllKeywords(): Flow<List<WatchedKeyword>>
    suspend fun addKeyword(keyword: String, intervalHours: Int)
    suspend fun deleteKeyword(keyword: WatchedKeyword)
    suspend fun updateLastRunAt(keyword: WatchedKeyword, timestamp: Long)
}

private class WatchedKeywordRepositoryImpl(context: Context) : WatchedKeywordRepository {
    private val dao = AppDatabase.getDatabase(context).watchedKeywordDao()

    override fun getAllKeywords(): Flow<List<WatchedKeyword>> = dao.getAllKeywords()

    override suspend fun addKeyword(keyword: String, intervalHours: Int) {
        dao.insertKeyword(WatchedKeyword(keyword = keyword, intervalHours = intervalHours))
    }

    override suspend fun deleteKeyword(keyword: WatchedKeyword) = dao.deleteKeyword(keyword)

    override suspend fun updateLastRunAt(keyword: WatchedKeyword, timestamp: Long) {
        dao.updateKeyword(keyword.copy(lastRunAt = timestamp))
    }
}

fun WatchedKeywordRepository(context: Context): WatchedKeywordRepository =
    WatchedKeywordRepositoryImpl(context)
