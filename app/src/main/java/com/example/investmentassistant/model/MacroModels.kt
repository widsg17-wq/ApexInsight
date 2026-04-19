package com.example.investmentassistant.model

import com.google.gson.annotations.SerializedName

// FRED API 전체 응답 구조
data class FredResponse(
    val observations: List<FredObservation>
)

// 개별 데이터 포인트 (날짜와 값)
data class FredObservation(
    val date: String,
    val value: String // FRED는 수치를 문자열로 보내므로 나중에 숫자로 변환 필요
)

// 지표별 고유 ID 정리 (FRED에서 약속된 이름들)
object FredSeriesId {
    const val US_10Y_YIELD = "DGS10"       // 미 국채 10년물 금리
    const val US_2Y_YIELD = "DGS2"         // 미 국채 2년물 금리
    const val FED_BALANCE_SHEET = "WALCL"  // 연준 대차대조표 (유동성)
    const val M2_MONEY_SUPPLY = "M2SL"     // M2 통화량
    const val REAL_INTEREST_RATE = "REAINTRATREARAT10Y" // 실질 금리
}