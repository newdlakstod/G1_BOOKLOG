package com.g1.booklog.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.g1.booklog.data.database.BookDatabase
import com.g1.booklog.data.repository.BookRepository
import com.g1.booklog.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.first

class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = BookDatabase.getDatabase(applicationContext)
            val repo = BookRepository(db.bookDao(), db.readingRecordDao())
            val firebaseRepo = FirebaseRepository()
            val books = repo.allBooks.first()
            firebaseRepo.uploadBooks(books)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
