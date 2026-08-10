package com.g1.booklog.data.network

import com.google.gson.annotations.SerializedName
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class KakaoBookResponse(
    @SerializedName("documents") val documents: List<KakaoBook>? = null
)

data class KakaoBook(
    @SerializedName("title") val title: String = "",
    @SerializedName("authors") val authors: List<String>? = null,
    @SerializedName("publisher") val publisher: String = "",
    @SerializedName("datetime") val datetime: String = "",
    // "ISBN10 ISBN13" 공백 구분 (둘 중 하나만 올 수도 있음)
    @SerializedName("isbn") val isbn: String = "",
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

    /** 제목/저자로 국내 도서 검색. 결과를 GoogleBookItem 형태로 변환해 UI 재사용. 키 미설정 시 빈 목록. */
    suspend fun searchBooks(query: String): List<GoogleBookItem> =
        documentsFor(query).map { it.toGoogleBookItem() }

    /** 제목으로 국내 도서 표지 썸네일 목록. 키 미설정 시 빈 목록. (에러는 호출부에서 처리) */
    suspend fun coverThumbnails(query: String): List<String> =
        documentsFor(query).map { hiRes(it.thumbnail) }.filter { it.isNotBlank() }

    private suspend fun documentsFor(query: String): List<KakaoBook> {
        if (BooksApiKeys.KAKAO_REST_API_KEY.isBlank() || query.isBlank()) return emptyList()
        val docs = service.search(query = query).documents.orEmpty()
        // 제목에 오탈자 공백("세종 의 나라")이 있으면 0건 → 공백 제거 후 재시도
        if (docs.isEmpty()) {
            val collapsed = query.replace(Regex("\\s+"), "")
            if (collapsed.isNotBlank() && collapsed != query) {
                return service.search(query = collapsed).documents.orEmpty()
            }
        }
        return docs
    }

    private fun KakaoBook.toGoogleBookItem(): GoogleBookItem {
        val ids = isbn.split(" ").filter { it.isNotBlank() }.map { code ->
            IndustryIdentifier(
                type = if (code.length == 13) "ISBN_13" else "ISBN_10",
                identifier = code
            )
        }
        return GoogleBookItem(
            VolumeInfo(
                title = title,
                authors = authors,
                publisher = publisher,
                publishedDate = datetime,   // "2008-05-30T..." → getYear()가 앞 4자리 사용
                industryIdentifiers = ids,
                imageLinks = ImageLinks(thumbnail = hiRes(thumbnail))
            )
        )
    }

    // 카카오 썸네일(R120x174)은 화질이 낮음 → URL 안의 원본 이미지(fname)를 꺼내 https 고화질로.
    private fun hiRes(thumbnail: String): String {
        val original = thumbnail.toHttpUrlOrNull()?.queryParameter("fname")
        return (original ?: thumbnail).replace("http://", "https://")
    }
}
