package com.g1.booklog.data.database.dao

import androidx.room.*
import com.g1.booklog.data.model.ReadingRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingRecordDao {

    @Query("SELECT * FROM reading_records WHERE bookId = :bookId ORDER BY date DESC")
    fun getRecordsByBook(bookId: Long): Flow<List<ReadingRecord>>

    @Query("SELECT SUM(pagesRead) FROM reading_records WHERE bookId = :bookId")
    fun getTotalPagesRead(bookId: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ReadingRecord): Long

    @Delete
    suspend fun deleteRecord(record: ReadingRecord)

    @Query("DELETE FROM reading_records WHERE bookId = :bookId")
    suspend fun deleteAllRecordsForBook(bookId: Long)
}
