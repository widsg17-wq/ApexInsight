package com.example.investmentassistant.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 'object'로 선언하면 앱 전체에서 딱 하나만 존재하는 공용 장부가 됩니다.
object TokenManager {
    private val _totalTokens = MutableStateFlow(0)
    val totalTokens: StateFlow<Int> = _totalTokens.asStateFlow()

    // AI가 답변을 줄 때마다 이 함수를 불러서 토큰을 누적합니다.
    fun addTokens(count: Int) {
        _totalTokens.value += count
    }
}