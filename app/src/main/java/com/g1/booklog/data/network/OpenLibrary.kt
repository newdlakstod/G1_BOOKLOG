package com.g1.booklog.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object OpenLibrary {
    private val client = OkHttpClient()

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
}
