package com.example.investmentassistant.api

import com.example.investmentassistant.model.NewsArticle
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
interface ApiService {
    suspend fun searchNews(query: String): List<NewsArticle>
}

class MockApiService : ApiService {
    override suspend fun searchNews(query: String): List<NewsArticle> {
        // Simulate network delay
        delay(1000)

        if (query.isBlank()) return emptyList()

        return listOf(
            NewsArticle(
                title = "Tech stocks surge as AI demand grows ($query)",
                source = "Financial Times",
                publishedAt = "2023-10-27T10:00:00Z",
                urlToImage = "https://picsum.photos/seed/tech/400/200",
                url = "https://example.com/news/1"
            ),
            NewsArticle(
                title = "Federal Reserve signals potential rate cuts next year",
                source = "Bloomberg",
                publishedAt = "2023-10-26T15:30:00Z",
                urlToImage = "https://picsum.photos/seed/fed/400/200",
                url = "https://example.com/news/2"
            ),
            NewsArticle(
                title = "New $query policies announced by government",
                source = "Reuters",
                publishedAt = "2023-10-26T08:15:00Z",
                urlToImage = null,
                url = "https://example.com/news/3"
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
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("apiKey") apiKey: String = "27aaddd3f2c44918a6e2bc31beb350d7" // Your API Key!
    ): NewsApiResponse
}

class RealApiService : ApiService {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://newsapi.org/v2/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val network = retrofit.create(NewsApiNetwork::class.java)

    override suspend fun searchNews(query: String): List<NewsArticle> {
        return try {
            val response = network.getNews(query)
            response.articles.map { apiArticle ->
                NewsArticle(
                    title = apiArticle.title ?: "No Title",
                    source = apiArticle.source?.name ?: "Unknown",
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