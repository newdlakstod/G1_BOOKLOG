package com.g1.booklog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReadingStatus(val label: String) {
    WANT_TO_READ("독서예정"),
    READING("독서중"),
    COMPLETED("독서완료")
}

enum class BookGenre(val label: String) {
    FICTION("소설"),
    NON_FICTION("비문학"),
    ESSAY("에세이"),
    SELF_HELP("자기계발"),
    SCIENCE("과학"),
    HISTORY("역사"),
    BIOGRAPHY("전기"),
    POETRY("시"),
    COMIC("만화"),
    OTHER("기타")
}

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val publisher: String = "",
    val publishYear: Int? = null,
    val totalPages: Int? = null,
    val genre: BookGenre = BookGenre.OTHER,
    val status: ReadingStatus = ReadingStatus.WANT_TO_READ,
    val coverImageUrl: String = "",
    val isbn: String = "",
    val rating: Float = 0f,
    val review: String = "",
    val memo: String = "",
    val highlights: String = "",
    val currentPage: Int = 0,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0
)
