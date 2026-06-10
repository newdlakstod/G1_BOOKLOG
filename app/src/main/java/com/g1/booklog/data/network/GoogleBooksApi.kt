package com.g1.booklog.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private val htmlTagRegex = Regex("<[^>]+>")

data class GoogleBookItem(
    @SerializedName("title") val titleText: String = "",
    @SerializedName("author") val authorText: String = "",
    @SerializedName("publisher") val publisherText: String = "",
    @SerializedName("pubdate") val pubdate: String = "",
    @SerializedName("isbn") val isbnText: String = "",
    @SerializedName("image") val imageUrl: String = "",
    @SerializedName("description") val descriptionText: String = ""
) {
    fun getTitle(): String = titleText.replace(htmlTagRegex, "")
    fun getAuthor(): String = authorText.replace(htmlTagRegex, "")
    fun getPublisher(): String = publisherText
    fun getYear(): Int? = pubdate.take(4).toIntOrNull()
    fun getIsbn13(): String = isbnText.split(" ").firstOrNull { it.length == 13 } ?: isbnText
    fun getThumbnail(): String = imageUrl
    fun getDescription(): String = descriptionText.replace(htmlTagRegex, "")
    fun getPageCount(): Int? = null
}

data class GoogleBooksResponse(
    @SerializedName("total") val totalItems: Int = 0,
    @SerializedName("items") val items: List<GoogleBookItem>? = null
)

interface GoogleBooksService {
    @GET("v1/search/book.json")
    suspend fun searchBooks(
        @Query("query") query: String,
        @Query("display") maxResults: Int = 20,
        @Query("sort") sort: String = "sim"
    ): GoogleBooksResponse
}

object GoogleBooksApi {
    val service: GoogleBooksService = Retrofit.Builder()
        .baseUrl("https://openapi.naver.com/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader("X-Naver-Client-Id", NaverApiKeys.CLIENT_ID)
                            .addHeader("X-Naver-Client-Secret", NaverApiKeys.CLIENT_SECRET)
                            .build()
                    )
                }
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GoogleBooksService::class.java)
}
