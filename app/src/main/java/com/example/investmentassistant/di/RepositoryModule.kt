package com.example.investmentassistant.di

import android.content.Context
import com.example.investmentassistant.data.CalendarEventDao
import com.example.investmentassistant.data.WatchedKeywordDao
import com.example.investmentassistant.data.WatchlistDao
import com.example.investmentassistant.data.repository.AiRepository
import com.example.investmentassistant.data.repository.CalendarRepository
import com.example.investmentassistant.data.repository.MacroRepository
import com.example.investmentassistant.data.repository.NewsRepository
import com.example.investmentassistant.data.repository.ReportRepository
import com.example.investmentassistant.data.repository.WatchedKeywordRepository
import com.example.investmentassistant.data.repository.WatchlistRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMacroRepository(): MacroRepository = MacroRepository()

    @Provides
    @Singleton
    fun provideNewsRepository(): NewsRepository = NewsRepository()

    @Provides
    @Singleton
    fun provideAiRepository(): AiRepository = AiRepository()

    @Provides
    @Singleton
    fun provideReportRepository(@ApplicationContext context: Context): ReportRepository =
        ReportRepository(context)

    @Provides
    @Singleton
    fun provideWatchedKeywordRepository(dao: WatchedKeywordDao): WatchedKeywordRepository =
        WatchedKeywordRepository(dao)

    @Provides
    @Singleton
    fun provideWatchlistRepository(dao: WatchlistDao): WatchlistRepository =
        WatchlistRepository(dao)

    @Provides
    @Singleton
    fun provideCalendarRepository(dao: CalendarEventDao): CalendarRepository =
        CalendarRepository(dao)
}
