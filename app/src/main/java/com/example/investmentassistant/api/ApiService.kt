package com.example.investmentassistant.api

import com.example.investmentassistant.model.NewsArticle
import com.example.investmentassistant.BuildConfig
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ==========================================
// 1. 기존 News API 관련 코드 (수정 없음)
// ==========================================

interface ApiService {
    suspend fun searchNews(query: String, fromDate: String? = null, toDate: String? = null): List<NewsArticle>
}

class MockApiService : ApiService {
    override suspend fun searchNews(query: String, fromDate: String?, toDate: String?): List<NewsArticle> {
        delay(1000)
        if (query.isBlank()) return emptyList()
        return listOf(
            NewsArticle(
                title = "Tech stocks surge as AI demand grows ($query)",
                source = "Financial Times",
                publishedAt = "2023-10-27T10:00:00Z",
                urlToImage = "https://picsum.photos/seed/tech/400/200",
                url = "https://example.com/news/1"
            )
        )
    }
}

data class NewsApiResponse(val articles: List<ApiArticle>)
data class ApiArticle(
    val title: String?,
    val source: ApiSource?,
    val publishedAt: String?,
    val urlToImage: String?,
    val url: String?
)
data class ApiSource(val name: String?)

interface NewsApiNetwork {
    @GET("everything")
    suspend fun getNews(
        @Query("q") query: String,
        @Query("from") fromDate: String? = null,
        @Query("to") toDate: String? = null,
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("apiKey") apiKey: String = "27aaddd3f2c44918a6e2bc31beb350d7"
    ): NewsApiResponse
}

class RealApiService : ApiService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://newsapi.org/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val network = retrofit.create(NewsApiNetwork::class.java)

    override suspend fun searchNews(query: String, fromDate: String?, toDate: String?): List<NewsArticle> {
        return try {
            val response = network.getNews(query, fromDate, toDate)
            response.articles.map { apiArticle ->
                NewsArticle(
                    title = apiArticle.title ?: "제목 없음",
                    source = apiArticle.source?.name ?: "알 수 없는 출처",
                    publishedAt = apiArticle.publishedAt ?: "",
                    urlToImage = apiArticle.urlToImage,
                    url = apiArticle.url ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

// ==========================================
// 2. 신규 추가된 FRED API 관련 코드
// ==========================================
data class FredResponse(val observations: List<FredObservation>)
data class FredObservation(val date: String, val value: String)

data class MacroData(val latestValue: String, val history: List<Float>)

interface FredApiNetwork {
    @GET("series/observations")
    suspend fun getObservations(
        @Query("series_id") seriesId: String,
        @Query("api_key") apiKey: String,
        @Query("file_type") fileType: String = "json",
        @Query("sort_order") sortOrder: String = "desc",
        @Query("limit") limit: Int = 30
    ): FredResponse
}

interface FredApiService {
    // ★ 수정: limit 파라미터 추가
    suspend fun getMacroData(seriesId: String, limit: Int = 30): MacroData
}

class RealFredApiService : FredApiService {
    private val apiKey = BuildConfig.FRED_API_KEY
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stlouisfed.org/fred/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val network = retrofit.create(FredApiNetwork::class.java)

    // ★ 수정: 파라미터로 받은 limit을 network 호출 시 넘겨줍니다
    override suspend fun getMacroData(seriesId: String, limit: Int): MacroData {
        return try {
            val response = network.getObservations(seriesId = seriesId, apiKey = apiKey, limit = limit)
            val obs = response.observations

            val latest = obs.firstOrNull()?.value ?: "-"
            val history = obs.mapNotNull { it.value.toFloatOrNull() }.reversed()

            MacroData(latest, history)
        } catch (e: Exception) {
            e.printStackTrace()
            MacroData("Error", emptyList())
        }
    }
}

// ==========================================
// 3. 야후 파이낸스 API
// ==========================================
data class YahooResponse(val chart: YahooChart?)
data class YahooChart(val result: List<YahooResult>?)
data class YahooResult(val meta: YahooMeta?, val indicators: YahooIndicators?)
data class YahooMeta(val regularMarketPrice: Float?, val chartPreviousClose: Float?)
data class YahooIndicators(val quote: List<YahooQuote>?)
data class YahooQuote(val close: List<Float?>?)

interface YahooApiNetwork {
    @GET("chart/{symbol}")
    suspend fun getChart(
        @retrofit2.http.Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "1mo"
    ): YahooResponse
}

interface FinanceApiService {
    // ★ 수정: interval과 range 파라미터 추가
    suspend fun getIndexData(symbol: String, interval: String = "1d", range: String = "1mo"): MacroData
}

class RealFinanceApiService : FinanceApiService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://query1.finance.yahoo.com/v8/finance/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val network = retrofit.create(YahooApiNetwork::class.java)

    // ★ 수정: 파라미터로 받은 interval과 range를 network 호출 시 넘겨줍니다
    override suspend fun getIndexData(symbol: String, interval: String, range: String): MacroData {
        return try {
            val response = network.getChart(symbol, interval = interval, range = range)
            val result = response.chart?.result?.firstOrNull()

            val currentPrice = result?.meta?.regularMarketPrice ?: 0f
            val history = result?.indicators?.quote?.firstOrNull()?.close?.filterNotNull() ?: emptyList()

            val formattedPrice = if (currentPrice > 0) String.format("%,.2f", currentPrice) else "Error"

            MacroData(formattedPrice, history)
        } catch (e: Exception) {
            e.printStackTrace()
            MacroData("Error", emptyList())
        }
    }
}

// ==========================================
// 4. Fear & Greed API
// ==========================================
data class FngResponse(val data: List<FngData>?)
data class FngData(val value: String?, val value_classification: String?)

interface FngApiNetwork {
    @GET("fng/")
    suspend fun getFearAndGreed(@Query("limit") limit: Int = 30): FngResponse
}

interface FngApiService {
    // ★ 수정: limit 파라미터 추가
    suspend fun getFearGreedData(limit: Int = 30): MacroData
}

class RealFngApiService : FngApiService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.alternative.me/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val network = retrofit.create(FngApiNetwork::class.java)

    // ★ 수정: 파라미터로 받은 limit을 network 호출 시 넘겨줍니다
    override suspend fun getFearGreedData(limit: Int): MacroData {
        return try {
            val response = network.getFearAndGreed(limit = limit)
            val dataList = response.data ?: emptyList()

            val latest = dataList.firstOrNull()?.value ?: "50"
            val classification = dataList.firstOrNull()?.value_classification ?: "Neutral"

            val history = dataList.mapNotNull { it.value?.toFloatOrNull() }.reversed()

            MacroData(latest, history)
        } catch (e: Exception) {
            e.printStackTrace()
            MacroData("Error", emptyList())
        }
    }
}