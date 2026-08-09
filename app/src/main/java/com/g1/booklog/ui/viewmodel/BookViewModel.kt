package com.g1.booklog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.BookGenre
import com.g1.booklog.data.model.ReadingRecord
import com.g1.booklog.data.model.ReadingStatus
import com.g1.booklog.data.network.GoogleBookItem
import com.g1.booklog.data.network.GoogleBooksApi
import com.g1.booklog.data.network.OpenLibrary
import com.g1.booklog.data.repository.BookRepository
import com.g1.booklog.data.repository.FirebaseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class BookUiState(
    val allBooks: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: ReadingStatus? = null,
    val wantToReadCount: Int = 0,
    val readingCount: Int = 0,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class BookViewModel(
    private val repository: BookRepository,
    private val firebaseRepo: FirebaseRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<ReadingStatus?>(null)

    private val _myDisplayName = MutableStateFlow("")
    val myDisplayName: StateFlow<String> = _myDisplayName.asStateFlow()

    private val _myPhotoUrl = MutableStateFlow("")
    val myPhotoUrl: StateFlow<String> = _myPhotoUrl.asStateFlow()

    val archivedYears: StateFlow<List<Int>> = repository.getArchivedYears()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repository.archiveOldCompletedBooks() }
        viewModelScope.launch {
            _myDisplayName.value = firebaseRepo.getMyNickname()
            _myPhotoUrl.value = firebaseRepo.getMyPhotoUrl()
        }
    }

    val uiState: StateFlow<BookUiState> = combine(
        repository.allBooks,
        _searchQuery,
        _selectedStatus,
        repository.countByStatus(ReadingStatus.WANT_TO_READ),
        repository.countByStatus(ReadingStatus.READING)
    ) { books, query, status, wantCount, readingCount ->
        val filtered = books.filter { book ->
            val matchesQuery = query.isEmpty() ||
                    book.title.contains(query, ignoreCase = true) ||
                    book.author.contains(query, ignoreCase = true)
            val matchesStatus = status == null || book.status == status
            matchesQuery && matchesStatus
        }
        BookUiState(
            allBooks = books,
            filteredBooks = filtered,
            searchQuery = query,
            selectedStatus = status,
            wantToReadCount = wantCount,
            readingCount = readingCount,
            completedCount = books.count { it.status == ReadingStatus.COMPLETED },
            totalCount = books.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookUiState()
    )

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setStatusFilter(status: ReadingStatus?) { _selectedStatus.value = status }

    // ── Naver Book Search ──────────────────────────────
    private val _naverResults = MutableStateFlow<List<GoogleBookItem>>(emptyList())
    val naverResults: StateFlow<List<GoogleBookItem>> = _naverResults.asStateFlow()

    private val _naverSearching = MutableStateFlow(false)
    val naverSearching: StateFlow<Boolean> = _naverSearching.asStateFlow()

    private val _naverError = MutableStateFlow<String?>(null)
    val naverError: StateFlow<String?> = _naverError.asStateFlow()

    // 검색 화면 → 추가 화면으로 선택한 책을 전달하는 임시 홀더
    private var _selectedNaverBook = MutableStateFlow<GoogleBookItem?>(null)
    val selectedNaverBook: StateFlow<GoogleBookItem?> = _selectedNaverBook.asStateFlow()

    fun searchNaverBooks(query: String) = viewModelScope.launch {
        if (query.isBlank()) return@launch
        _naverSearching.value = true
        _naverError.value = null
        try {
            val response = GoogleBooksApi.service.searchBooks(query = query.trim())
            val items = response.items ?: emptyList()
            _naverResults.value = items
            if (items.isEmpty()) _naverError.value = "검색 결과가 없습니다"
        } catch (e: Exception) {
            _naverError.value = "검색 오류: ${e.javaClass.simpleName}: ${e.message ?: "null"}"
            _naverResults.value = emptyList()
        } finally {
            _naverSearching.value = false
        }
    }

    fun selectNaverBook(book: GoogleBookItem) { _selectedNaverBook.value = book }
    fun clearSelectedNaverBook() { _selectedNaverBook.value = null }
    fun clearNaverSearch() {
        _naverResults.value = emptyList()
        _naverError.value = null
    }

    // 표지만 따로 검색 (Google Books 판본 표지 + Open Library ISBN 표지)
    private val _coverCandidates = MutableStateFlow<List<String>>(emptyList())
    val coverCandidates: StateFlow<List<String>> = _coverCandidates.asStateFlow()

    private val _coverSearching = MutableStateFlow(false)
    val coverSearching: StateFlow<Boolean> = _coverSearching.asStateFlow()

    fun searchCovers(title: String, isbn: String) = viewModelScope.launch {
        _coverSearching.value = true
        val urls = LinkedHashSet<String>()
        val query = if (title.isNotBlank()) title.trim() else if (isbn.isNotBlank()) "isbn:$isbn" else ""
        if (query.isNotBlank()) {
            try {
                GoogleBooksApi.service.searchBooks(query = query).items.orEmpty().forEach { item ->
                    item.getThumbnail().takeIf { it.isNotBlank() }?.let { urls.add(it) }
                }
            } catch (_: Exception) { /* 표지 검색 실패는 무시 */ }
        }
        OpenLibrary.coverUrlForIsbn(isbn.trim())?.let { urls.add(it) }
        if (title.isNotBlank()) urls.addAll(OpenLibrary.coverUrlsByTitle(title.trim()))
        _coverCandidates.value = urls.toList()
        _coverSearching.value = false
    }

    fun clearCoverCandidates() { _coverCandidates.value = emptyList() }

    fun getBookById(id: Long): Flow<Book?> = repository.getBookById(id)
    fun getRecordsByBook(bookId: Long): Flow<List<ReadingRecord>> = repository.getRecordsByBook(bookId)
    fun getArchivedBooks(year: Int): Flow<List<Book>> = repository.getArchivedBooks(year)

    fun getMonthlyCompletedBooks(): Flow<List<Book>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfMonth = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val endOfMonth = cal.timeInMillis
        return repository.getBooksCompletedInMonth(startOfMonth, endOfMonth)
    }

    fun getMonthlyReadingStats(): Flow<List<Pair<String, Int>>> =
        getMonthlyReadingStatsForYear(Calendar.getInstance().get(Calendar.YEAR))

    fun getMonthlyReadingStatsForYear(year: Int): Flow<List<Pair<String, Int>>> {
        val booksFlow = if (year == Calendar.getInstance().get(Calendar.YEAR)) {
            repository.allBooks
        } else {
            repository.getArchivedBooks(year)
        }
        return booksFlow.map { books ->
            (1..12).map { month ->
                val cal = Calendar.getInstance()
                cal.set(year, month - 1, 1, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis
                val count = books.count {
                    it.status == ReadingStatus.COMPLETED &&
                    it.endDate != null &&
                    it.endDate in start until end
                }
                "${month}월" to count
            }
        }
    }

    fun getGenreStats(): Flow<Map<BookGenre, Int>> = repository.allBooks.map { books ->
        books.filter { it.status == ReadingStatus.COMPLETED }
            .groupBy { it.genre }
            .mapValues { it.value.size }
    }

    fun addBookFromSearch(
        item: GoogleBookItem,
        status: ReadingStatus,
        coverUrl: String = item.getThumbnail()
    ) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        repository.insertBook(
            Book(
                title = item.getTitle(),
                author = item.getAuthor(),
                publisher = item.getPublisher(),
                publishYear = item.getYear(),
                totalPages = item.getPageCount(),
                isbn = item.getIsbn13(),
                coverImageUrl = coverUrl,
                status = status,
                startDate = if (status == ReadingStatus.READING || status == ReadingStatus.COMPLETED) now else null,
                endDate = if (status == ReadingStatus.COMPLETED) now else null
            )
        )
    }

    fun addBook(book: Book) = viewModelScope.launch {
        repository.insertBook(book)
    }

    fun updateBook(book: Book) = viewModelScope.launch {
        repository.updateBook(book)
    }

    fun updateStatus(book: Book, status: ReadingStatus) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        repository.updateBook(
            book.copy(
                status = status,
                startDate = if (status == ReadingStatus.READING && book.startDate == null) now else book.startDate,
                endDate = if (status == ReadingStatus.COMPLETED) now else book.endDate
            )
        )
    }

    fun updateBooksOrder(books: List<Book>) = viewModelScope.launch {
        repository.updateBooksOrder(books)
    }

    fun deleteBook(book: Book) = viewModelScope.launch {
        repository.deleteBook(book)
    }

    fun addReadingRecord(bookId: Long, pagesRead: Int, note: String = "") = viewModelScope.launch {
        repository.insertRecord(
            ReadingRecord(bookId = bookId, pagesRead = pagesRead, note = note)
        )
        val book = repository.getBookById(bookId).first()
        book?.let {
            repository.updateBook(
                it.copy(
                    currentPage = it.currentPage + pagesRead,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteRecord(record: ReadingRecord) = viewModelScope.launch {
        repository.deleteRecord(record)
    }

    // ── Cloud Sync ─────────────────────────────────────
    private val _syncState = MutableStateFlow<String?>(null)
    val syncState: StateFlow<String?> = _syncState.asStateFlow()

    fun uploadToCloud() = viewModelScope.launch {
        _syncState.value = "업로드 중..."
        try {
            val books = repository.allBooksIncludingArchived.first()
            firebaseRepo.uploadBooks(books)
            _syncState.value = "업로드 완료 (${books.size}권)"
        } catch (e: Exception) {
            _syncState.value = "업로드 실패: ${e.localizedMessage}"
        }
    }

    fun downloadFromCloud() = viewModelScope.launch {
        _syncState.value = "다운로드 중..."
        try {
            val books = firebaseRepo.downloadBooks()
            repository.replaceAllBooks(books)
            _syncState.value = "다운로드 완료 (${books.size}권)"
        } catch (e: Exception) {
            _syncState.value = "다운로드 실패: ${e.localizedMessage}"
        }
    }

    fun autoBackup() = viewModelScope.launch {
        try {
            val books = repository.allBooksIncludingArchived.first()
            firebaseRepo.uploadBooks(books)
        } catch (_: Exception) {}
    }

    fun autoRestore() = viewModelScope.launch {
        try {
            val books = firebaseRepo.downloadBooks()
            if (books.isNotEmpty()) repository.replaceAllBooks(books)
        } catch (_: Exception) {}
    }

    class Factory(
        private val repository: BookRepository,
        private val firebaseRepo: FirebaseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
                return BookViewModel(repository, firebaseRepo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
