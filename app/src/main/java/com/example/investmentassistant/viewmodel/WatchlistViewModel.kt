package com.example.investmentassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.repository.WatchlistRepository
import com.example.investmentassistant.model.WatchlistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WatchlistUiState(
    val isRefreshing: Boolean = false,
    val addError: String? = null,
    val addSuccess: Boolean = false,
)

class WatchlistViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WatchlistRepository(AppDatabase.getDatabase(app).watchlistDao())

    val items: StateFlow<List<WatchlistItem>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    fun addSymbol(symbol: String) {
        if (symbol.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(addError = null, addSuccess = false)
            val added = repo.addSymbol(symbol.trim())
            if (added) {
                _uiState.value = _uiState.value.copy(addSuccess = true)
                refresh()
            } else {
                _uiState.value = _uiState.value.copy(addError = "이미 추가된 종목입니다.")
            }
        }
    }

    fun removeItem(id: Long) {
        viewModelScope.launch { repo.removeItem(id) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            repo.refreshQuotes()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun clearAddState() {
        _uiState.value = _uiState.value.copy(addError = null, addSuccess = false)
    }
}
