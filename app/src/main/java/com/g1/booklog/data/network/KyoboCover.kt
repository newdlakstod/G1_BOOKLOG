package com.g1.booklog.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object KyoboCover {
    private val client = OkHttpClient()

    // 교보 '이미지 없음' 플레이스홀더 크기(458x0 템플릿). 이 크기면 실제 표지가 없는 것.
    private const val NO_IMAGE_BYTES = 34150L

    /** ISBN13 교보 표지. 실제 표지가 있으면 URL, 없으면(플레이스홀더) null. 키 불필요. */
    suspend fun coverUrlForIsbn(isbn: String): String? = withContext(Dispatchers.IO) {
        val clean = isbn.filter { it.isDigit() }
        if (clean.length != 13) return@withContext null
        val url = "https://contents.kyobobook.co.kr/sih/fit-in/458x0/pdt/$clean.jpg"
        try {
            client.newCall(Request.Builder().url(url).head().build()).execute().use { resp ->
                val len = resp.header("Content-Length")?.toLongOrNull()
                if (resp.isSuccessful && len != null && len != NO_IMAGE_BYTES) url else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
