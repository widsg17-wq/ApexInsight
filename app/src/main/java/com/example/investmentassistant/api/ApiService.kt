package com.example.investmentassistant.api

import com.example.investmentassistant.model.NewsArticle
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
interface ApiService {
    suspend fun searchNews(query: String, fromDate: String? = null, toDate: String? = null): List<NewsArticle>
}

class MockApiService : ApiService {
    override suspend fun searchNews(query: String, fromDate: String?, toDate: String?): List<NewsArticle> {
        kotlinx.coroutines.delay(1000)
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
        @Query("from") fromDate: String? = null, // 추가됨
        @Query("to") toDate: String? = null,     // 추가됨
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
            val response = network.getNews(query, fromDate, toDate) // 파라미터 전달
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