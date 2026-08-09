package com.g1.booklog.data.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

object OpenLibrary {
    private val client = OkHttpClient()
    private val gson = Gson()

    /** ISBN에 해당하는 표지가 실제로 있으면 이미지 URL, 없으면 null. */
    suspend fun coverUrlForIsbn(isbn: String): String? = withContext(Dispatchers.IO) {
        if (isbn.isBlank()) return@withContext null
        val url = "https://covers.openlibrary.org/b/isbn/$isbn-L.jpg"
        // default=false면 표지가 없을 때 404 → 존재하는 경우만 채택
        val req = Request.Builder().url("$url?default=false").head().build()
        try {
            client.newCall(req).execute().use { if (it.isSuccessful) url else null }
        } catch (_: Exception) {
            null
        }
    }

    /** 제목으로 검색해 여러 판본의 표지 URL을 반환. */
    suspend fun coverUrlsByTitle(title: String, limit: Int = 8): List<String> = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext emptyList()
        val url = "https://openlibrary.org/search.json".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("title", title)
            ?.addQueryParameter("fields", "cover_i")
            ?.addQueryParameter("limit", limit.toString())
            ?.build() ?: return@withContext emptyList()
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val body = resp.body?.string() ?: return@withContext emptyList()
                gson.fromJson(body, OlSearchResponse::class.java).docs.orEmpty()
                    .mapNotNull { it.coverId }
                    .distinct()
                    .map { "https://covers.openlibrary.org/b/id/$it-L.jpg" }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private data class OlSearchResponse(
        @SerializedName("docs") val docs: List<OlDoc>? = null
    )

    private data class OlDoc(
        @SerializedName("cover_i") val coverId: Long? = null
    )
}
