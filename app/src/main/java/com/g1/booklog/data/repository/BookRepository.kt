package com.g1.booklog.data.repository

import com.g1.booklog.data.database.dao.BookDao
import com.g1.booklog.data.database.dao.ReadingRecordDao
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.ReadingRecord
import com.g1.booklog.data.model.ReadingStatus
import kotlinx.coroutines.flow.Flow

class BookRepository(
    private val bookDao: BookDao,
    private val recordDao: ReadingRecordDao
) {
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    fun getBooksByStatus(status: ReadingStatus) = bookDao.getBooksByStatus(status)
    fun getBookById(id: Long) = bookDao.getBookById(id)
    fun searchBooks(query: String) = bookDao.searchBooks(query)
    fun countByStatus(status: ReadingStatus) = bookDao.countByStatus(status)
    fun countAll() = bookDao.countAll()
    fun getBooksCompletedInMonth(startOfMonth: Long, endOfMonth: Long) =
        bookDao.getBooksCompletedInMonth(startOfMonth, endOfMonth)

    fun getRecordsByBook(bookId: Long) = recordDao.getRecordsByBook(bookId)
    fun getTotalPagesRead(bookId: Long) = recordDao.getTotalPagesRead(bookId)

    suspend fun insertBook(book: Book): Long = bookDao.insertBook(book)
    suspend fun updateBook(book: Book) = bookDao.updateBook(book.copy(updatedAt = System.currentTimeMillis()))
    suspend fun updateBooksOrder(books: List<Book>) {
        books.forEachIndexed { index, book ->
            bookDao.updateOrderIndex(book.id, index)
        }
    }
    suspend fun deleteBook(book: Book) = bookDao.deleteBook(book)

    suspend fun replaceAllBooks(books: List<Book>) {
        bookDao.deleteAllBooks()
        books.forEach { bookDao.insertBook(it) }
    }

    suspend fun insertRecord(record: ReadingRecord): Long = recordDao.insertRecord(record)
    suspend fun deleteRecord(record: ReadingRecord) = recordDao.deleteRecord(record)
}
