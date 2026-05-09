package com.example.investmentassistant.viewmodel

import android.app.Application
import android.content.Context
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
import com.example.investmentassistant.worker.IndicatorAlertWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WatchedKeywordsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = WatchedKeywordRepository(app)
    private val workManager = WorkManager.getInstance(app)
    private val prefs = app.getSharedPreferences("alert_prefs", Context.MODE_PRIVATE)

    val keywords: StateFlow<List<WatchedKeyword>> = repository.getAllKeywords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _indicatorAlertEnabled = MutableStateFlow(
        prefs.getBoolean("indicator_alert_enabled", false)
    )
    val indicatorAlertEnabled: StateFlow<Boolean> = _indicatorAlertEnabled.asStateFlow()

    fun addKeyword(keyword: String, intervalHours: Int) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            repository.addKeyword(keyword.trim(), intervalHours)
            scheduleAutoReportWorker()
        }
    }

    fun deleteKeyword(keyword: WatchedKeyword) {
        viewModelScope.launch { repository.deleteKeyword(keyword) }
    }

    fun toggleIndicatorAlert(enabled: Boolean) {
        _indicatorAlertEnabled.value = enabled
        prefs.edit().putBoolean("indicator_alert_enabled", enabled).apply()
        if (enabled) scheduleIndicatorAlertWorker()
        else workManager.cancelUniqueWork(IndicatorAlertWorker.WORK_NAME)
    }

    private fun scheduleAutoReportWorker() {
        val request = PeriodicWorkRequestBuilder<AutoReportWorker>(1, TimeUnit.HOURS)
            .setConstraints(networkConstraints())
            .build()
        workManager.enqueueUniquePeriodicWork(
            AutoReportWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleIndicatorAlertWorker() {
        val request = PeriodicWorkRequestBuilder<IndicatorAlertWorker>(1, TimeUnit.HOURS)
            .setConstraints(networkConstraints())
            .build()
        workManager.enqueueUniquePeriodicWork(
            IndicatorAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}
