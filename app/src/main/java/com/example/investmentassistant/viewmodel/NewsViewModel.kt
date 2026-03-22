package com.example.investmentassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.investmentassistant.api.ApiService
import com.example.investmentassistant.api.MockApiService
import com.example.investmentassistant.api.RealApiService // ★ 이 한 줄이 추가되었습니다!
import com.example.investmentassistant.model.NewsArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewsViewModel(
    // 드디어 진짜 배달부(RealApiService)를 공식적으로 고용합니다!
    private val apiService: ApiService = RealApiService()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _newsList = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsList: StateFlow<List<NewsArticle>> = _newsList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchNews() {
        val query = _searchQuery.value
        if (query.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = apiService.searchNews(query)
                _newsList.value = results
            } catch (e: Exception) {
                _newsList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}