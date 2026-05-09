package com.example.investmentassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.investmentassistant.data.WatchedKeyword
import com.example.investmentassistant.data.repository.WatchedKeywordRepository
import com.example.investmentassistant.worker.AutoReportWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WatchedKeywordsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = WatchedKeywordRepository(app)

    val keywords: StateFlow<List<WatchedKeyword>> = repository.getAllKeywords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addKeyword(keyword: String, intervalHours: Int) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            repository.addKeyword(keyword.trim(), intervalHours)
            scheduleWorker()
        }
    }

    fun deleteKeyword(keyword: WatchedKeyword) {
        viewModelScope.launch { repository.deleteKeyword(keyword) }
    }

    private fun scheduleWorker() {
        val request = PeriodicWorkRequestBuilder<AutoReportWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
            AutoReportWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
