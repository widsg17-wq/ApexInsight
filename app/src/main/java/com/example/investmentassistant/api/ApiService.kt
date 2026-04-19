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

// ★ 새로 추가: 뷰모델로 보낼 '최신값'과 '과거 추세 리스트'를 묶은 상자
data class MacroData(val latestValue: String, val history: List<Float>)

interface FredApiNetwork {
    @GET("series/observations")
    suspend fun getObservations(
        @Query("series_id") seriesId: String,
        @Query("api_key") apiKey: String,
        @Query("file_type") fileType: String = "json",
        @Query("sort_order") sortOrder: String = "desc",
        @Query("limit") limit: Int = 30 // ★ 1개에서 30개로 늘렸습니다! (그래프를 그리기 위해)
    ): FredResponse
}

interface FredApiService {
    // 반환값을 String에서 MacroData 상자로 바꿨습니다.
    suspend fun getMacroData(seriesId: String): MacroData
}

class RealFredApiService : FredApiService {
    private val apiKey = BuildConfig.FRED_API_KEY
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.stlouisfed.org/fred/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val network = retrofit.create(FredApiNetwork::class.java)

    override suspend fun getMacroData(seriesId: String): MacroData {
        return try {
            val response = network.getObservations(seriesId = seriesId, apiKey = apiKey)
            val obs = response.observations

            // 1. 최신값 (가장 첫 번째 데이터)
            val latest = obs.firstOrNull()?.value ?: "-"

            // 2. 과거 30일치 데이터 (소수점으로 변환 후, 그래프 그리기 좋게 과거->현재 순으로 뒤집음)
            val history = obs.mapNotNull { it.value.toFloatOrNull() }.reversed()

            MacroData(latest, history)
        } catch (e: Exception) {
            e.printStackTrace()
            MacroData("Error", emptyList())
        }
    }
}

data class YahooResponse(val chart: YahooChart?)
data class YahooChart(val result: List<YahooResult>?)
data class YahooResult(val meta: YahooMeta?, val indicators: YahooIndicators?)
data class YahooMeta(val regularMarketPrice: Float?, val chartPreviousClose: Float?)
data class YahooIndicators(val quote: List<YahooQuote>?)
data class YahooQuote(val close: List<Float?>?)

interface YahooApiNetwork {
    // interval=1d (하루 단위), range=1mo (최근 1달치) 데이터 요청
    @GET("chart/{symbol}")
    suspend fun getChart(
        @retrofit2.http.Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "1mo"
    ): YahooResponse
}

interface FinanceApiService {
    suspend fun getIndexData(symbol: String): MacroData
}

class RealFinanceApiService : FinanceApiService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://query1.finance.yahoo.com/v8/finance/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val network = retrofit.create(YahooApiNetwork::class.java)

    override suspend fun getIndexData(symbol: String): MacroData {
        return try {
            val response = network.getChart(symbol)
            val result = response.chart?.result?.firstOrNull()

            // 1. 최신 현재가 가져오기
            val currentPrice = result?.meta?.regularMarketPrice ?: 0f

            // 2. 과거 한 달 치 종가 리스트 가져오기 (null 값은 제외)
            val history = result?.indicators?.quote?.firstOrNull()?.close?.filterNotNull() ?: emptyList()

            // 3. 천 단위 콤마(,) 찍어서 예쁘게 포맷팅
            val formattedPrice = if (currentPrice > 0) String.format("%,.2f", currentPrice) else "Error"

            MacroData(formattedPrice, history)
        } catch (e: Exception) {
            e.printStackTrace()
            MacroData("Error", emptyList())
        }
    }
}

data class FngResponse(val data: List<FngData>?)
data class FngData(val value: String?, val value_classification: String?)

interface FngApiNetwork {
    @GET("fng/")
    suspend fun getFearAndGreed(@Query("limit") limit: Int = 30): FngResponse
}

interface FngApiService {
    suspend fun getFearGreedData(): MacroData
}

class RealFngApiService : FngApiService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.alternative.me/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val network = retrofit.create(FngApiNetwork::class.java)

    override suspend fun getFearGreedData(): MacroData {
        return try {
            val response = network.getFearAndGreed(limit = 30)
            val dataList = response.data ?: emptyList()

            // 1. 최신 수치
            val latest = dataList.firstOrNull()?.value ?: "50"
            // (선택) "Greed", "Fear" 같은 텍스트 데이터 (필요시 사용)
            val classification = dataList.firstOrNull()?.value_classification ?: "Neutral"

            // 2. 과거 30일치 추세 (과거->현재 순으로 정렬)
            val history = dataList.mapNotNull { it.value?.toFloatOrNull() }.reversed()

            MacroData(latest, history)
        } catch (e: Exception) {
            e.printStackTrace()
            MacroData("Error", emptyList())
        }
    }
}