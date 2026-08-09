package com.g1.booklog.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class KakaoBookResponse(
    @SerializedName("documents") val documents: List<KakaoBook>? = null
)

data class KakaoBook(
    @SerializedName("thumbnail") val thumbnail: String = ""
)

interface KakaoBookService {
    @GET("v3/search/book")
    suspend fun search(
        @Query("query") query: String,
        @Query("size") size: Int = 10
    ): KakaoBookResponse
}

object KakaoBookSearch {
    private val service: KakaoBookService = Retrofit.Builder()
        .baseUrl("https://dapi.kakao.com/")
        .client(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader("Authorization", "KakaoAK ${BooksApiKeys.KAKAO_REST_API_KEY}")
                            .build()
                    )
                }
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(KakaoBookService::class.java)

    /** 제목으로 국내 도서 표지 썸네일 목록. 키 미설정 시 빈 목록. (에러는 호출부에서 처리) */
    suspend fun coverThumbnails(query: String): List<String> {
        if (BooksApiKeys.KAKAO_REST_API_KEY.isBlank() || query.isBlank()) return emptyList()
        return service.search(query = query)
            .documents.orEmpty()
            .map { it.thumbnail }
            .filter { it.isNotBlank() }
    }
}
