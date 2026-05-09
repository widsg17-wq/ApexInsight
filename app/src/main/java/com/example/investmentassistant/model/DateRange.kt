package com.example.investmentassistant.model

enum class DateRange(val label: String) {
    ALL("전체"),
    PAST_WEEK("최근 1주일"),
    PAST_MONTH("최근 1개월"),
    CUSTOM("직접 설정"),
}
