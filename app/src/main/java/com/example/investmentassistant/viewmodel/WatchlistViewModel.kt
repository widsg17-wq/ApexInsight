package com.example.investmentassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.investmentassistant.api.FinnhubSymbolResult
import com.example.investmentassistant.data.repository.WatchlistRepository
import com.example.investmentassistant.model.WatchlistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistUiState(
    val isRefreshing: Boolean = false,
    val addError: String? = null,
    val addSuccess: Boolean = false,
    val refreshError: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repo: WatchlistRepository,
) : ViewModel() {

    val items: StateFlow<List<WatchlistItem>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FinnhubSymbolResult>>(emptyList())
    val searchResults: StateFlow<List<FinnhubSymbolResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .filter { it.length >= 2 }
                .collect { query ->
                    _isSearching.value = true
                    _searchResults.value = repo.searchSymbols(query)
                    _isSearching.value = false
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length < 2) _searchResults.value = emptyList()
    }

    fun addFromSearch(result: FinnhubSymbolResult) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(addError = null, addSuccess = false)
            val added = repo.addFromSearch(result)
            if (added) {
                _uiState.value = _uiState.value.copy(addSuccess = true)
                refresh()
            } else {
                _uiState.value = _uiState.value.copy(addError = "이미 추가된 종목입니다.")
            }
        }
    }

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
            _uiState.value = _uiState.value.copy(isRefreshing = true, refreshError = null)
            try {
                repo.refreshQuotes()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    refreshError = "시세 갱신 실패: ${e.localizedMessage ?: "네트워크 오류"}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    fun clearRefreshError() { _uiState.value = _uiState.value.copy(refreshError = null) }

    fun clearAddState() {
        _uiState.value = _uiState.value.copy(addError = null, addSuccess = false)
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
}
