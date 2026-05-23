package com.example.investmentassistant.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY symbol ASC")
    fun getAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist ORDER BY symbol ASC")
    suspend fun getAllOnce(): List<WatchlistEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WatchlistEntity): Long

    @Update
    suspend fun update(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM watchlist WHERE symbol = :symbol")
    suspend fun countBySymbol(symbol: String): Int
}
