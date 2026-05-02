package com.example.investmentassistant.utils

import android.content.Context
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.TokenRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TokenManager {
    // 앱 어디서든 이 함수만 부르면 알아서 DB에 오늘 날짜로 토큰을 누적해 줍니다!
    fun addTokensToDb(context: Context, usedTokens: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())

            // 1. 오늘 날짜 기록이 있는지 확인
            val existingRecord = db.tokenDao().getTokensByDate(today)
            val currentTotal = existingRecord?.totalTokens ?: 0

            // 2. 기존 값에 방금 쓴 토큰을 더해서 덮어쓰기
            db.tokenDao().insertTokenRecord(
                TokenRecord(date = today, totalTokens = currentTotal + usedTokens)
            )
        }
    }
}