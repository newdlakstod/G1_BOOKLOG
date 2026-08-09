package com.g1.booklog.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class GoogleBookItem(
    @SerializedName("volumeInfo") val volumeInfo: VolumeInfo = VolumeInfo()
) {
    fun getTitle(): String = volumeInfo.title
    fun getAuthor(): String = volumeInfo.authors?.joinToString(", ").orEmpty()
    fun getPublisher(): String = volumeInfo.publisher
    fun getYear(): Int? = volumeInfo.publishedDate.take(4).toIntOrNull()
    fun getIsbn13(): String {
        val ids = volumeInfo.industryIdentifiers.orEmpty()
        return ids.firstOrNull { it.type == "ISBN_13" }?.identifier
            ?: ids.firstOrNull { it.type == "ISBN_10" }?.identifier
            ?: ""
    }
    // Google 썸네일 링크는 종종 http로 오므로 https로 승격 (cleartext 설정 불필요)
    fun getThumbnail(): String =
        (volumeInfo.imageLinks?.thumbnail ?: volumeInfo.imageLinks?.smallThumbnail ?: "")
            .replace("http://", "https://")
    fun getPageCount(): Int? = volumeInfo.pageCount?.takeIf { it > 0 }
}

data class VolumeInfo(
    @SerializedName("title") val title: String = "",
    @SerializedName("authors") val authors: List<String>? = null,
    @SerializedName("publisher") val publisher: String = "",
    @SerializedName("publishedDate") val publishedDate: String = "",
    @SerializedName("industryIdentifiers") val industryIdentifiers: List<IndustryIdentifier>? = null,
    @SerializedName("pageCount") val pageCount: Int? = null,
    @SerializedName("imageLinks") val imageLinks: ImageLinks? = null
)

data class IndustryIdentifier(
    @SerializedName("type") val type: String = "",
    @SerializedName("identifier") val identifier: String = ""
)

data class ImageLinks(
    @SerializedName("smallThumbnail") val smallThumbnail: String? = null,
    @SerializedName("thumbnail") val thumbnail: String? = null
)

data class GoogleBooksResponse(
    @SerializedName("items") val items: List<GoogleBookItem>? = null
)

interface GoogleBooksService {
    @GET("books/v1/volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 20
    ): GoogleBooksResponse
}

object GoogleBooksApi {
    val service: GoogleBooksService = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val req = chain.request()
                    // API 키가 있으면 쿼리에 붙인다 (Google Books는 키 없이는 할당량 0)
                    val newReq = if (BooksApiKeys.GOOGLE_BOOKS_API_KEY.isNotBlank()) {
                        val url = req.url.newBuilder()
                            .addQueryParameter("key", BooksApiKeys.GOOGLE_BOOKS_API_KEY)
                            .build()
                        req.newBuilder().url(url).build()
                    } else req
                    chain.proceed(newReq)
                }
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = if (com.g1.booklog.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
                })
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GoogleBooksService::class.java)
}
