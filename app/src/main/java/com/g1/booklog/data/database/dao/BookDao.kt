package com.g1.booklog.data.database.dao

import androidx.room.*
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.ReadingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY orderIndex ASC, createdAt ASC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY orderIndex ASC, createdAt ASC")
    fun getBooksByStatus(status: ReadingStatus): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<Book?>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<Book>>

    @Query("SELECT COUNT(*) FROM books WHERE status = :status")
    fun countByStatus(status: ReadingStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM books")
    fun countAll(): Flow<Int>

    @Query("""
        SELECT * FROM books
        WHERE status = 'COMPLETED'
        AND endDate >= :startOfMonth
        AND endDate < :endOfMonth
        ORDER BY endDate DESC
    """)
    fun getBooksCompletedInMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<Book>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Query("UPDATE books SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, orderIndex: Int)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()
}
