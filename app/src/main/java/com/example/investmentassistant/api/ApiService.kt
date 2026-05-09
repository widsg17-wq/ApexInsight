package com.example.investmentassistant.api

import com.example.investmentassistant.BuildConfig
import com.example.investmentassistant.model.MacroData
import com.example.investmentassistant.model.NewsArticle
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ==========================================
// News API
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
                urlToImage = null,
                url = "https://example.com/news/1"
            )
        )
    }
}

data class NewsApiResponse(val articles: List<ApiArticle>)
data class ApiArticle(val title: String?, val source: ApiSource?, val publishedAt: String?, val urlToImage: String?, val url: String?)
data class ApiSource(val name: String?)

interface NewsApiNetwork {
    @GET("everything")
    suspend fun getNews(
        @Query("q") query: String,
        @Query("from") fromDate: String? = null,
        @Query("to") toDate: String? = null,
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("apiKey") apiKey: String = BuildConfig.NEWS_API_KEY,
    ): NewsApiResponse
}

class RealApiService : ApiService {
    private val network = Retrofit.Builder()
        .baseUrl("https://newsapi.org/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NewsApiNetwork::class.java)

    override suspend fun searchNews(query: String, fromDate: String?, toDate: String?): List<NewsArticle> =
        try {
            network.getNews(query, fromDate, toDate).articles.map {
                NewsArticle(
                    title = it.title ?: "제목 없음",
                    source = it.source?.name ?: "알 수 없는 출처",
                    publishedAt = it.publishedAt ?: "",
                    urlToImage = it.urlToImage,
                    url = it.url ?: "",
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
}

// ==========================================
// FRED API
// ==========================================

data class FredResponse(val observations: List<FredObservation>)
data class FredObservation(val date: String, val value: String)

interface FredApiNetwork {
    @GET("series/observations")
    suspend fun getObservations(
        @Query("series_id") seriesId: String,
        @Query("api_key") apiKey: String,
        @Query("file_type") fileType: String = "json",
        @Query("sort_order") sortOrder: String = "desc",
        @Query("limit") limit: Int = 30,
    ): FredResponse
}

interface FredApiService {
    suspend fun getMacroData(seriesId: String, limit: Int = 30): MacroData
}

class RealFredApiService : FredApiService {
    private val network = Retrofit.Builder()
        .baseUrl("https://api.stlouisfed.org/fred/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FredApiNetwork::class.java)

    override suspend fun getMacroData(seriesId: String, limit: Int): MacroData =
        try {
            val obs = network.getObservations(seriesId = seriesId, apiKey = BuildConfig.FRED_API_KEY, limit = limit).observations
            MacroData(obs.firstOrNull()?.value ?: "-", obs.mapNotNull { it.value.toFloatOrNull() }.reversed())
        } catch (e: Exception) {
            e.printStackTrace()
            MacroData("Error", emptyList())
        }
}

// ==========================================
// Yahoo Finance API
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
        @Query("range") range: String = "1mo",
    ): YahooResponse
}

interface FinanceApiService {
    suspend fun getIndexData(symbol: String, interval: String = "1d", range: String = "1mo"): MacroData
}

class RealFinanceApiService : FinanceApiService {
    private val network = Retrofit.Builder()
        .baseUrl("https://query1.finance.yahoo.com/v8/finance/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(YahooApiNetwork::class.java)

    override suspend fun getIndexData(symbol: String, interval: String, range: String): MacroData =
        try {
            val result = network.getChart(symbol, interval = interval, range = range).chart?.result?.firstOrNull()
            val currentPrice = result?.meta?.regularMarketPrice ?: 0f
            val history = result?.indicators?.quote?.firstOrNull()?.close?.filterNotNull() ?: emptyList()
            MacroData(if (currentPrice > 0) String.format("%,.2f", currentPrice) else "Error", history)
        } catch (e: Exception) {
            e.printStackTrace()
            MacroData("Error", emptyList())
        }
}

// ==========================================
// Fear & Greed API
// ==========================================

data class FngResponse(val data: List<FngData>?)
data class FngData(val value: String?, val value_classification: String?)

interface FngApiNetwork {
    @GET("fng/")
    suspend fun getFearAndGreed(@Query("limit") limit: Int = 30): FngResponse
}

interface FngApiService {
    suspend fun getFearGreedData(limit: Int = 30): MacroData
}

class RealFngApiService : FngApiService {
    private val network = Retrofit.Builder()
        .baseUrl("https://api.alternative.me/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FngApiNetwork::class.java)

    override suspend fun getFearGreedData(limit: Int): MacroData =
        try {
            val dataList = network.getFearAndGreed(limit = limit).data ?: emptyList()
            MacroData(
                latestValue = dataList.firstOrNull()?.value ?: "50",
                history = dataList.mapNotNull { it.value?.toFloatOrNull() }.reversed(),
            )
        } catch (e: Exception) {
            e.printStackTrace()
            MacroData("Error", emptyList())
        }
}
