package com.g1.booklog.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class CseResponse(
    @SerializedName("items") val items: List<CseItem>? = null
)

data class CseItem(
    @SerializedName("link") val link: String = ""
)

interface CustomSearchService {
    @GET("customsearch/v1")
    suspend fun searchImages(
        @Query("key") key: String,
        @Query("cx") cx: String,
        @Query("q") q: String,
        @Query("searchType") searchType: String = "image",
        @Query("num") num: Int = 8,
        @Query("safe") safe: String = "active"
    ): CseResponse
}

object GoogleImageSearch {
    private val service: CustomSearchService = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(CustomSearchService::class.java)

    /** 표지 이미지 URL 목록. 키/cx 미설정이거나 실패하면 빈 목록. */
    suspend fun coverImageUrls(query: String): List<String> {
        val key = BooksApiKeys.GOOGLE_BOOKS_API_KEY
        val cx = BooksApiKeys.GOOGLE_CSE_ID
        if (key.isBlank() || cx.isBlank() || query.isBlank()) return emptyList()
        return try {
            service.searchImages(key = key, cx = cx, q = query)
                .items.orEmpty()
                .map { it.link }
                .filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
