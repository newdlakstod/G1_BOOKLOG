package com.g1.booklog.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.g1.booklog.data.database.dao.BookDao
import com.g1.booklog.data.database.dao.ReadingRecordDao
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.BookGenre
import com.g1.booklog.data.model.ReadingRecord
import com.g1.booklog.data.model.ReadingStatus

class Converters {
    @TypeConverter
    fun fromReadingStatus(value: ReadingStatus): String = value.name

    @TypeConverter
    fun toReadingStatus(value: String): ReadingStatus = ReadingStatus.valueOf(value)

    @TypeConverter
    fun fromBookGenre(value: BookGenre): String = value.name

    @TypeConverter
    fun toBookGenre(value: String): BookGenre = BookGenre.valueOf(value)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE books ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE books SET orderIndex = id")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE books ADD COLUMN highlights TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE books ADD COLUMN archivedYear INTEGER")
    }
}

@Database(
    entities = [Book::class, ReadingRecord::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BookDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun readingRecordDao(): ReadingRecordDao

    companion object {
        @Volatile
        private var INSTANCE: BookDatabase? = null

        fun getDatabase(context: Context): BookDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    BookDatabase::class.java,
                    "booklog_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
            }
        }
    }
}
