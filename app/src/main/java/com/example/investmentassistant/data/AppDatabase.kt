package com.example.investmentassistant.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedReport::class, TokenRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao
    abstract fun tokenDao(): TokenDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "investment_assistant_db" // 내 핸드폰에 저장될 실제 DB 파일 이름
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}