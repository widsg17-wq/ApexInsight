package com.example.investmentassistant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IndicatorSnapshotDao {
    @Query("SELECT * FROM indicator_snapshots")
    suspend fun getAll(): List<IndicatorSnapshot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(snapshots: List<IndicatorSnapshot>)
}
