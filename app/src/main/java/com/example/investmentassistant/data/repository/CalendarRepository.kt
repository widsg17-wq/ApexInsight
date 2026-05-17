package com.example.investmentassistant.data.repository

import com.example.investmentassistant.BuildConfig
import com.example.investmentassistant.api.CalendarService
import com.example.investmentassistant.api.FinnhubEarningsEvent
import com.example.investmentassistant.api.FinnhubEconomicEvent
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
            .baseUrl("https://finnhub.io/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CalendarService::class.java)
    }

    private val kst = ZoneId.of("Asia/Seoul")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    suspend fun fetchAndCacheCalendar() {
        val apiKey = BuildConfig.FINNHUB_API_KEY
        if (apiKey.isBlank()) throw IllegalStateException("FINNHUB_API_KEY가 설정되지 않았습니다.")

        val today = LocalDate.now(kst)
        val from = today.minusDays(1).format(dateFmt)
        val to = today.plusDays(60).format(dateFmt)

        val events = mutableListOf<CalendarEvent>()
        val errors = mutableListOf<String>()

        try {
            val response = service.getEconomicCalendar(apiKey)
            events += (response.economicCalendar ?: emptyList())
                .filter { isHighImpact(it) }
                .mapNotNull { it.toCalendarEvent() }
        } catch (e: Exception) {
            errors += "경제 캘린더: ${e.message}"
        }

        try {
            val response = service.getEarningsCalendar(from, to, apiKey)
            events += (response.earningsCalendar ?: emptyList())
                .filter { it.symbol in MAJOR_TICKERS }
                .mapNotNull { it.toCalendarEvent() }
        } catch (e: Exception) {
            errors += "실적 캘린더: ${e.message}"
        }

        if (errors.size == 2) throw RuntimeException(errors.joinToString("\n"))

        if (events.isNotEmpty()) {
            val notifiedIds = dao.getNotifiedEventIds().toHashSet()
            dao.upsertAll(events.map { event ->
                event.toEntity().let {
                    if (it.id in notifiedIds) it.copy(isNotified = 1) else it
                }
            })
        }

        val cutoff = today.minusDays(14).atStartOfDay(kst).toInstant().toEpochMilli()
        dao.deleteOldEvents(cutoff)
    }

    suspend fun getEventsForDateRange(fromMs: Long, toMs: Long): List<CalendarEvent> =
        dao.getEventsBetween(fromMs, toMs).map { it.toModel() }

    suspend fun getUnnotifiedAnnouncedEvents(): List<CalendarEvent> =
        dao.getUnnotifiedAnnouncedEvents().map { it.toModel() }

    suspend fun markAsNotified(ids: List<String>) = dao.markAsNotified(ids)

    private fun isHighImpact(event: FinnhubEconomicEvent): Boolean {
        val isHigh = event.impact?.equals("high", ignoreCase = true) == true
        val isImportantKeyword = IMPORTANT_KEYWORDS.any { event.event.contains(it, ignoreCase = true) }
        return isHigh || isImportantKeyword
    }

    private fun FinnhubEconomicEvent.toCalendarEvent(): CalendarEvent? {
        val ms = parseDateTime(time) ?: return null
        val importance = when (impact?.lowercase()) {
            "high" -> EventImportance.HIGH
            "medium" -> EventImportance.MEDIUM
            else -> EventImportance.LOW
        }
        val effectiveUnit = unit?.takeIf { it.isNotBlank() }
            ?: if (PERCENTAGE_KEYWORDS.any { event.lowercase().contains(it) }) "%" else null
        return CalendarEvent(
            id = "eco_${time}_${event.replace(" ", "_")}",
            type = EventType.ECONOMIC,
            title = toKoreanTitle(event),
            country = country,
            scheduledAt = ms,
            previous = formatValue(prev, effectiveUnit),
            forecast = formatValue(estimate, effectiveUnit),
            actual = formatValue(actual, effectiveUnit),
            importance = importance,
        )
    }

    private fun FinnhubEarningsEvent.toCalendarEvent(): CalendarEvent? {
        val ms = parseDate(date) ?: return null
        val timeLabel = when (hour?.lowercase()) {
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
            epsActual = epsActual,
            epsEstimate = epsEstimate,
            revenueActual = revenueActual,
            revenueEstimate = revenueEstimate,
            importance = if (symbol in TOP_TICKERS) EventImportance.HIGH else EventImportance.MEDIUM,
        )
    }

    private fun formatValue(value: Double?, unit: String?): String? {
        value ?: return null
        val formatted = if (value == kotlin.math.floor(value) && !value.isInfinite()) {
            value.toLong().toString()
        } else {
            "%.2f".format(value)
        }
        return if (!unit.isNullOrBlank()) "$formatted$unit" else formatted
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

        val PERCENTAGE_KEYWORDS = listOf(
            "cpi", "ppi", "pce", "gdp", "unemployment", "inflation",
            "pmi", "ism", "retail sales", "rate", "nfp", "nonfarm", "payroll",
        )

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
            val suffix = when {
                lower.contains("mom") || lower.contains("m/m") -> " (월간)"
                lower.contains("yoy") || lower.contains("y/y") -> " (연간)"
                lower.contains("qoq") || lower.contains("q/q") -> " (분기)"
                else -> ""
            }
            EVENT_NAMES.forEach { (key, value) ->
                if (lower.contains(key)) return value + suffix
            }
            return event
        }
    }
}
