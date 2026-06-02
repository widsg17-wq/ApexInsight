package com.example.investmentassistant.data.repository

import com.example.investmentassistant.data.WatchedKeyword
import com.example.investmentassistant.data.WatchedKeywordDao
import kotlinx.coroutines.flow.Flow

interface WatchedKeywordRepository {
    fun getAllKeywords(): Flow<List<WatchedKeyword>>
    suspend fun addKeyword(keyword: String, intervalHours: Int)
    suspend fun deleteKeyword(keyword: WatchedKeyword)
    suspend fun updateLastRunAt(keyword: WatchedKeyword, timestamp: Long)
}

private class WatchedKeywordRepositoryImpl(private val dao: WatchedKeywordDao) : WatchedKeywordRepository {

    override fun getAllKeywords(): Flow<List<WatchedKeyword>> = dao.getAllKeywords()

    override suspend fun addKeyword(keyword: String, intervalHours: Int) {
        dao.insertKeyword(WatchedKeyword(keyword = keyword, intervalHours = intervalHours))
    }

    override suspend fun deleteKeyword(keyword: WatchedKeyword) = dao.deleteKeyword(keyword)

    override suspend fun updateLastRunAt(keyword: WatchedKeyword, timestamp: Long) {
        dao.updateKeyword(keyword.copy(lastRunAt = timestamp))
    }
}

fun WatchedKeywordRepository(dao: WatchedKeywordDao): WatchedKeywordRepository =
    WatchedKeywordRepositoryImpl(dao)
