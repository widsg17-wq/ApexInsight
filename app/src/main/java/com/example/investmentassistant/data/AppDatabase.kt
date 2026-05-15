package com.example.investmentassistant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SavedReport::class, TokenRecord::class, WatchedKeyword::class, IndicatorSnapshot::class, CalendarEventEntity::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao
    abstract fun tokenDao(): TokenDao
    abstract fun watchedKeywordDao(): WatchedKeywordDao
    abstract fun indicatorSnapshotDao(): IndicatorSnapshotDao
    abstract fun calendarEventDao(): CalendarEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS watched_keywords (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        keyword TEXT NOT NULL,
                        intervalHours INTEGER NOT NULL DEFAULT 24,
                        lastRunAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS indicator_snapshots (
                        key TEXT PRIMARY KEY NOT NULL,
                        value REAL NOT NULL,
                        savedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS calendar_events (
                        id TEXT PRIMARY KEY NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        country TEXT NOT NULL,
                        scheduledAt INTEGER NOT NULL,
                        previous TEXT,
                        forecast TEXT,
                        actual TEXT,
                        importance TEXT NOT NULL,
                        ticker TEXT,
                        earningsTime TEXT,
                        epsActual REAL,
                        epsEstimate REAL,
                        revenueActual INTEGER,
                        revenueEstimate INTEGER,
                        isNotified INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "investment_assistant_db",
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
