package com.example.investmentassistant.di

import android.content.Context
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.CalendarEventDao
import com.example.investmentassistant.data.IndicatorSnapshotDao
import com.example.investmentassistant.data.ReportDao
import com.example.investmentassistant.data.TokenDao
import com.example.investmentassistant.data.WatchedKeywordDao
import com.example.investmentassistant.data.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    fun provideReportDao(db: AppDatabase): ReportDao = db.reportDao()

    @Provides
    fun provideTokenDao(db: AppDatabase): TokenDao = db.tokenDao()

    @Provides
    fun provideWatchedKeywordDao(db: AppDatabase): WatchedKeywordDao = db.watchedKeywordDao()

    @Provides
    fun provideIndicatorSnapshotDao(db: AppDatabase): IndicatorSnapshotDao = db.indicatorSnapshotDao()

    @Provides
    fun provideCalendarEventDao(db: AppDatabase): CalendarEventDao = db.calendarEventDao()

    @Provides
    fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()
}
