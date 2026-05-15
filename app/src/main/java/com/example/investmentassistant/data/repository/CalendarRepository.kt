package com.example.investmentassistant.data.repository

import com.example.investmentassistant.BuildConfig
import com.example.investmentassistant.api.CalendarService
import com.example.investmentassistant.api.FmpEarningsEvent
import com.example.investmentassistant.api.FmpEconomicEvent
import com.example.investmentassistant.data.CalendarEventDao
import com.example.investmentassistant.data.toEntity
import com.example.investmentassistant.model.CalendarEvent
import com.example.investmentassistant.model.EventImportance
import com.example.investmentassistant.model.EventType
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CalendarRepository(private val dao: CalendarEventDao) {

    private val service: CalendarService by lazy {
        Retrofit.Builder()
            .baseUrl("https://financialmodelingprep.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CalendarService::class.java)
    }

    private val kst = ZoneId.of("Asia/Seoul")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun fetchAndCacheCalendar() {
        val apiKey = BuildConfig.FMP_API_KEY
        if (apiKey.isBlank()) return

        val today = LocalDate.now(kst)
        val from = today.minusDays(1).format(dateFmt)
        val to = today.plusDays(14).format(dateFmt)

        val events = mutableListOf<CalendarEvent>()

        try {
            val economic = service.getEconomicCalendar(from, to, apiKey)
            events += economic
                .filter { isHighImpact(it) }
                .mapNotNull { it.toCalendarEvent() }
        } catch (_: Exception) {}

        try {
            val earnings = service.getEarningsCalendar(from, to, apiKey)
            events += earnings
                .filter { it.symbol in MAJOR_TICKERS }
                .mapNotNull { it.toCalendarEvent() }
        } catch (_: Exception) {}

        if (events.isNotEmpty()) {
            dao.upsertAll(events.map { it.toEntity() })
        }

        val cutoff = today.minusDays(14).atStartOfDay(kst).toInstant().toEpochMilli()
        dao.deleteOldEvents(cutoff)
    }

    suspend fun getEventsForDateRange(fromMs: Long, toMs: Long): List<CalendarEvent> =
        dao.getEventsBetween(fromMs, toMs).map { it.toModel() }

    suspend fun getUnnotifiedAnnouncedEvents(): List<CalendarEvent> =
        dao.getUnnotifiedAnnouncedEvents().map { it.toModel() }

    suspend fun markAsNotified(ids: List<String>) = dao.markAsNotified(ids)

    private fun isHighImpact(event: FmpEconomicEvent): Boolean {
        val isHigh = event.impact?.equals("High", ignoreCase = true) == true
        val isImportantKeyword = IMPORTANT_KEYWORDS.any { event.event.contains(it, ignoreCase = true) }
        return isHigh || isImportantKeyword
    }

    private fun FmpEconomicEvent.toCalendarEvent(): CalendarEvent? {
        val ms = parseDateTime(date) ?: return null
        val importance = when (impact?.lowercase()) {
            "high" -> EventImportance.HIGH
            "medium" -> EventImportance.MEDIUM
            else -> EventImportance.LOW
        }
        return CalendarEvent(
            id = "eco_${date}_${event.replace(" ", "_")}",
            type = EventType.ECONOMIC,
            title = toKoreanTitle(event),
            country = country,
            scheduledAt = ms,
            previous = previous,
            forecast = estimate,
            actual = actual,
            importance = importance,
        )
    }

    private fun FmpEarningsEvent.toCalendarEvent(): CalendarEvent? {
        val ms = parseDate(date) ?: return null
        val timeLabel = when (time?.lowercase()) {
            "bmo" -> "장 시작 전"
            "amc" -> "장 마감 후"
            "dmh" -> "장중"
            else -> ""
        }
        return CalendarEvent(
            id = "earn_${date}_${symbol}",
            type = EventType.EARNINGS,
            title = "${getCompanyName(symbol)} 실적 발표",
            country = getCountry(symbol),
            scheduledAt = ms,
            ticker = symbol,
            earningsTime = timeLabel,
            epsActual = eps,
            epsEstimate = epsEstimated,
            revenueActual = revenue,
            revenueEstimate = revenueEstimated,
            importance = if (symbol in TOP_TICKERS) EventImportance.HIGH else EventImportance.MEDIUM,
        )
    }

    private fun parseDateTime(dateStr: String): Long? = try {
        if (dateStr.length > 10) {
            LocalDateTime.parse(dateStr, dateTimeFmt)
                .atZone(ZoneId.of("UTC"))
                .toInstant().toEpochMilli()
        } else {
            LocalDate.parse(dateStr, dateFmt)
                .atStartOfDay(kst).toInstant().toEpochMilli()
        }
    } catch (_: Exception) { null }

    private fun parseDate(dateStr: String): Long? = try {
        LocalDate.parse(dateStr, dateFmt).atStartOfDay(kst).toInstant().toEpochMilli()
    } catch (_: Exception) { null }

    companion object {
        val MAJOR_TICKERS = setOf(
            "NVDA", "AAPL", "MSFT", "GOOGL", "GOOG", "AMZN", "META", "TSLA",
            "AMD", "INTC", "QCOM", "TSM", "ASML", "AVGO",
            "JPM", "GS", "BAC", "MS",
            "XOM", "CVX",
            "005930.KS", "000660.KS",
        )

        val TOP_TICKERS = setOf("NVDA", "AAPL", "MSFT", "GOOGL", "AMZN", "META", "TSLA", "005930.KS")

        val IMPORTANT_KEYWORDS = listOf(
            "CPI", "PPI", "Fed", "FOMC", "GDP", "NFP", "Unemployment",
            "PCE", "ISM", "Retail Sales", "Rate Decision", "Interest Rate",
            "Inflation", "Payroll", "Nonfarm",
        )

        private val COMPANY_NAMES = mapOf(
            "NVDA" to "엔비디아", "AAPL" to "애플", "MSFT" to "마이크로소프트",
            "GOOGL" to "구글", "GOOG" to "구글", "AMZN" to "아마존",
            "META" to "메타", "TSLA" to "테슬라", "AMD" to "AMD",
            "INTC" to "인텔", "QCOM" to "퀄컴", "TSM" to "TSMC",
            "ASML" to "ASML", "AVGO" to "브로드컴",
            "JPM" to "JP모건", "GS" to "골드만삭스",
            "BAC" to "뱅크오브아메리카", "MS" to "모건스탠리",
            "XOM" to "엑슨모빌", "CVX" to "셰브론",
            "005930.KS" to "삼성전자", "000660.KS" to "SK하이닉스",
        )

        private val EVENT_NAMES = mapOf(
            "cpi" to "소비자물가지수 (CPI)",
            "ppi" to "생산자물가지수 (PPI)",
            "core cpi" to "근원 CPI",
            "core pce" to "근원 PCE",
            "pce" to "개인소비지출 (PCE)",
            "fomc" to "FOMC 회의",
            "fed" to "연준 기준금리",
            "interest rate" to "기준금리 결정",
            "rate decision" to "기준금리 결정",
            "gdp" to "GDP 성장률",
            "nonfarm" to "비농업 고용",
            "nfp" to "비농업 고용",
            "unemployment" to "실업률",
            "payroll" to "고용 보고서",
            "ism manufacturing" to "ISM 제조업지수",
            "ism services" to "ISM 서비스지수",
            "ism" to "ISM 지수",
            "retail sales" to "소매판매",
            "inflation" to "인플레이션",
            "pmi" to "PMI 지수",
        )

        fun getCompanyName(ticker: String) = COMPANY_NAMES[ticker] ?: ticker

        fun getCountry(ticker: String) = when {
            ticker.endsWith(".KS") -> "KR"
            ticker.endsWith(".T") -> "JP"
            else -> "US"
        }

        fun toKoreanTitle(event: String): String {
            val lower = event.lowercase()
            EVENT_NAMES.forEach { (key, value) ->
                if (lower.contains(key)) return value
            }
            return event
        }
    }
}
