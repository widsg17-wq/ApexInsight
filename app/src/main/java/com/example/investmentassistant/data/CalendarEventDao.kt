package com.example.investmentassistant.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface CalendarEventDao {

    @Query("SELECT * FROM calendar_events WHERE scheduledAt >= :fromMs AND scheduledAt < :toMs ORDER BY scheduledAt ASC")
    suspend fun getEventsBetween(fromMs: Long, toMs: Long): List<CalendarEventEntity>

    @Query("SELECT * FROM calendar_events WHERE isNotified = 0 AND (actual IS NOT NULL OR epsActual IS NOT NULL)")
    suspend fun getUnnotifiedAnnouncedEvents(): List<CalendarEventEntity>

    @Upsert
    suspend fun upsertAll(events: List<CalendarEventEntity>)

    @Query("UPDATE calendar_events SET isNotified = 1 WHERE id IN (:ids)")
    suspend fun markAsNotified(ids: List<String>)

    @Query("DELETE FROM calendar_events WHERE scheduledAt < :beforeMs")
    suspend fun deleteOldEvents(beforeMs: Long)
}
