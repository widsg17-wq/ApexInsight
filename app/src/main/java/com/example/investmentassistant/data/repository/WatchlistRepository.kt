package com.example.investmentassistant.data.repository

import com.example.investmentassistant.BuildConfig
import com.example.investmentassistant.api.FinnhubNewsItem
import com.example.investmentassistant.api.FinnhubQuote
import com.example.investmentassistant.api.FinnhubSymbolResult
import com.example.investmentassistant.api.MarketService
import com.example.investmentassistant.api.YahooFinanceService
import com.example.investmentassistant.data.KoreanStockDatabase
import com.example.investmentassistant.data.WatchlistDao
import com.example.investmentassistant.data.WatchlistEntity
import com.example.investmentassistant.model.WatchlistItem
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class WatchlistAlert(
    val item: WatchlistItem,
    val quote: FinnhubQuote,
    val analysis: String,
)

class WatchlistRepository(private val dao: WatchlistDao) {

    private val apiKey get() = BuildConfig.FINNHUB_API_KEY
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private val service: MarketService by lazy {
        Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MarketService::class.java)
    }

    private val yahooService: YahooFinanceService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                )
            }
            .build()
        Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(YahooFinanceService::class.java)
    }

    private val gemini by lazy {
        GenerativeModel(modelName = "gemini-2.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    }

    fun getAll(): Flow<List<WatchlistItem>> = dao.getAll().map { list -> list.map { it.toModel() } }

    suspend fun searchSymbols(query: String): List<FinnhubSymbolResult> {
        val hasKorean = query.any { it.code in 0xAC00..0xD7A3 || it.code in 0x1100..0x11FF }
        if (hasKorean) return KoreanStockDatabase.search(query)
        return try {
            yahooService.searchSymbols(query).quotes
                .filter { it.quoteType in setOf("EQUITY", "ETF", "INDEX", "MUTUALFUND", "FUND") }
                .take(15)
                .map { quote ->
                    val typeLabel = when (quote.quoteType) {
                        "EQUITY" -> "주식"
                        "ETF" -> "ETF"
                        "INDEX" -> "지수"
                        "MUTUALFUND", "FUND" -> "펀드"
                        else -> quote.quoteType
                    }
                    FinnhubSymbolResult(
                        symbol = quote.symbol,
                        displaySymbol = quote.symbol,
                        description = quote.longname.ifBlank { quote.shortname },
                        type = typeLabel,
                    )
                }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun addSymbol(symbol: String): Boolean {
        if (dao.countBySymbol(symbol.uppercase()) > 0) return false
        val upper = symbol.uppercase()
        val displayName = try {
            val result = service.searchSymbol(upper, apiKey)
            result.result.firstOrNull { it.symbol == upper }?.description ?: upper
        } catch (_: Exception) { upper }
        val exchange = when {
            upper.endsWith(".KS") || upper.endsWith(".KQ") -> "KR"
            upper.startsWith("^") -> "INDEX"
            else -> "US"
        }
        dao.insert(WatchlistEntity(symbol = upper, displayName = displayName, exchange = exchange))
        return true
    }

    suspend fun addFromSearch(result: FinnhubSymbolResult): Boolean {
        val symbol = result.symbol.uppercase()
        if (dao.countBySymbol(symbol) > 0) return false
        val exchange = when {
            symbol.endsWith(".KS") || symbol.endsWith(".KQ") -> "KR"
            symbol.startsWith("^") -> "INDEX"
            else -> "US"
        }
        dao.insert(WatchlistEntity(
            symbol = symbol,
            displayName = result.description.ifBlank { symbol },
            exchange = exchange,
        ))
        return true
    }

    suspend fun removeItem(id: Long) = dao.deleteById(id)

    suspend fun refreshQuotes(): List<WatchlistAlert> {
        val items = dao.getAllOnce()
        val now = System.currentTimeMillis()
        val alerts = mutableListOf<WatchlistAlert>()

        for (entity in items) {
            try {
                val isKorean = entity.symbol.endsWith(".KS") || entity.symbol.endsWith(".KQ")
                val (price, changePercent) = if (isKorean) {
                    fetchYahooQuote(entity.symbol)
                } else {
                    val q = service.getQuote(entity.symbol, apiKey)
                    Pair(q.c, q.dp)
                }

                if (price == 0.0) {
                    dao.update(entity.copy(lastCheckedAt = now))
                    continue
                }

                val isAlert = Math.abs(changePercent) >= entity.threshold
                    && now - entity.lastAlertAt > 60 * 60 * 1000

                val finnhubQuote = FinnhubQuote(c = price, dp = changePercent)
                var alertMessage: String? = entity.lastAlertMessage
                if (isAlert) {
                    val news = fetchRecentNews(entity.symbol)
                    alertMessage = analyzeAlert(entity.symbol, entity.displayName, finnhubQuote, news)
                    alerts += WatchlistAlert(entity.toModel(), finnhubQuote, alertMessage)
                }

                dao.update(entity.copy(
                    lastPrice = price,
                    lastChangePercent = changePercent,
                    lastCheckedAt = now,
                    lastAlertAt = if (isAlert) now else entity.lastAlertAt,
                    lastAlertMessage = alertMessage,
                ))
            } catch (_: Exception) {
                dao.update(entity.copy(lastCheckedAt = now))
            }
        }
        return alerts
    }

    private suspend fun fetchYahooQuote(symbol: String): Pair<Double, Double> = try {
        val result = yahooService.getQuote(symbol).quoteResponse.result.firstOrNull()
        Pair(result?.regularMarketPrice ?: 0.0, result?.regularMarketChangePercent ?: 0.0)
    } catch (_: Exception) { Pair(0.0, 0.0) }

    private suspend fun fetchRecentNews(symbol: String): List<FinnhubNewsItem> = try {
        val today = ZonedDateTime.now().format(dateFmt)
        val threeDaysAgo = ZonedDateTime.now().minusDays(3).format(dateFmt)
        service.getCompanyNews(symbol, threeDaysAgo, today, apiKey).take(5)
    } catch (_: Exception) { emptyList() }

    private suspend fun analyzeAlert(
        symbol: String,
        name: String,
        quote: FinnhubQuote,
        news: List<FinnhubNewsItem>,
    ): String {
        val direction = if (quote.dp > 0) "급등" else "급락"
        val newsText = if (news.isEmpty()) "관련 뉴스 없음" else
            news.joinToString("\n") { "- ${it.headline}" }

        val prompt = """
            너는 주식 시장 전문가야. 다음 종목의 $direction 원인을 간결하게 분석해줘.

            종목: $name ($symbol)
            현재가: ${"%.2f".format(quote.c)}
            변동률: ${"%.2f".format(quote.dp)}% ($direction)
            시가: ${"%.2f".format(quote.o)} / 전일 종가: ${"%.2f".format(quote.pc)}

            최근 뉴스:
            $newsText

            3~4문장으로 $direction 원인과 투자자 주의사항을 한국어로 답변해.
        """.trimIndent()

        return try {
            gemini.generateContent(prompt).text ?: "분석 실패"
        } catch (_: Exception) { "분석 중 오류가 발생했습니다." }
    }

    companion object {
        fun isMarketOpen(): Boolean {
            val now = ZonedDateTime.now()
            val dow = now.dayOfWeek
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false

            val etTime = now.withZoneSameInstant(ZoneId.of("America/New_York")).toLocalTime()
            if (etTime.isAfter(LocalTime.of(9, 29)) && etTime.isBefore(LocalTime.of(16, 1))) return true

            val kstTime = now.withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalTime()
            if (kstTime.isAfter(LocalTime.of(8, 59)) && kstTime.isBefore(LocalTime.of(15, 31))) return true

            return false
        }
    }
}

private fun WatchlistEntity.toModel() = WatchlistItem(
    id = id, symbol = symbol, displayName = displayName, exchange = exchange,
    threshold = threshold, lastPrice = lastPrice, lastChangePercent = lastChangePercent,
    lastCheckedAt = lastCheckedAt, lastAlertAt = lastAlertAt, lastAlertMessage = lastAlertMessage,
)
