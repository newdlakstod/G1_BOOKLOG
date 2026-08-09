package com.g1.booklog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.BookGenre
import com.g1.booklog.data.model.ReadingStatus
import com.g1.booklog.ui.viewmodel.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit,
    onNaverSearch: () -> Unit = {}
) {
    BookFormScreen(
        title = "책 추가",
        initialBook = null,
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onNaverSearch = onNaverSearch,
        onSave = { book ->
            viewModel.addBook(book)
            onNavigateBack()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookScreen(
    bookId: Long,
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit
) {
    val book by viewModel.getBookById(bookId).collectAsState(initial = null)

    if (book != null) {
        BookFormScreen(
            title = "책 수정",
            initialBook = book,
            viewModel = viewModel,
            onNavigateBack = onNavigateBack,
            onSave = { updatedBook ->
                viewModel.updateBook(updatedBook)
                onNavigateBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookFormScreen(
    title: String,
    initialBook: Book?,
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit,
    onSave: (Book) -> Unit,
    onNaverSearch: (() -> Unit)? = null
) {
    var bookTitle by remember { mutableStateOf(initialBook?.title ?: "") }
    var author by remember { mutableStateOf(initialBook?.author ?: "") }
    var publisher by remember { mutableStateOf(initialBook?.publisher ?: "") }
    var totalPages by remember { mutableStateOf(initialBook?.totalPages?.toString() ?: "") }
    var publishYear by remember { mutableStateOf(initialBook?.publishYear ?.toString() ?: "") }
    var selectedGenre by remember { mutableStateOf(initialBook?.genre ?: BookGenre.OTHER) }
    var selectedStatus by remember { mutableStateOf(initialBook?.status ?: ReadingStatus.WANT_TO_READ) }
    var isbn by remember { mutableStateOf(initialBook?.isbn ?: "") }
    var coverImageUrl by remember { mutableStateOf(initialBook?.coverImageUrl ?: "") }

    var titleError by remember { mutableStateOf(false) }
    var authorError by remember { mutableStateOf(false) }

    var genreExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    // 표지 검색 다이얼로그
    var showCoverDialog by remember { mutableStateOf(false) }
    val coverCandidates by viewModel.coverCandidates.collectAsState()
    val coverSearching by viewModel.coverSearching.collectAsState()
    val coverDebug by viewModel.coverDebug.collectAsState()

    if (showCoverDialog) {
        CoverPickerDialog(
            candidates = coverCandidates,
            loading = coverSearching,
            onPick = { coverImageUrl = it; showCoverDialog = false; viewModel.clearCoverCandidates() },
            onDismiss = { showCoverDialog = false; viewModel.clearCoverCandidates() },
            debug = coverDebug
        )
    }

    // 네이버 검색에서 선택한 책으로 자동 채우기
    val selectedNaverBook by viewModel.selectedNaverBook.collectAsState()
    LaunchedEffect(selectedNaverBook) {
        selectedNaverBook?.let { item ->
            bookTitle = item.getTitle()
            author = item.getAuthor()
            publisher = item.getPublisher()
            publishYear = item.getYear()?.toString() ?: ""
            isbn = item.getIsbn13()
            coverImageUrl = item.getThumbnail()
            viewModel.clearSelectedNaverBook()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        titleError = bookTitle.isBlank()
                        authorError = author.isBlank()
                        if (!titleError && !authorError) {
                            val now = System.currentTimeMillis()
                            val base = initialBook ?: Book(title = "", author = "")
                            onSave(
                                base.copy(
                                    title = bookTitle.trim(),
                                    author = author.trim(),
                                    publisher = publisher.trim(),
                                    totalPages = totalPages.toIntOrNull(),
                                    publishYear = publishYear.toIntOrNull(),
                                    genre = selectedGenre,
                                    status = selectedStatus,
                                    isbn = isbn.trim(),
                                    coverImageUrl = coverImageUrl,
                                    startDate = when {
                                        base.startDate != null -> base.startDate
                                        selectedStatus == ReadingStatus.READING || selectedStatus == ReadingStatus.COMPLETED -> now
                                        else -> null
                                    },
                                    endDate = when {
                                        base.endDate != null -> base.endDate
                                        selectedStatus == ReadingStatus.COMPLETED -> now
                                        else -> null
                                    }
                                )
                            )
                        }
                    }) {
                        Icon(Icons.Default.Check, "저장")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 네이버 검색 버튼 (AddBook 화면에서만 표시)
            if (onNaverSearch != null) {
                OutlinedButton(
                    onClick = onNaverSearch,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("책 검색하기")
                }
            }

            // 표지 미리보기 + 표지 검색
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val coverModifier = Modifier
                    .width(88.dp)
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(8.dp))
                if (coverImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = coverImageUrl,
                        contentDescription = "표지",
                        modifier = coverModifier,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    BookCoverPlaceholder(title = bookTitle, modifier = coverModifier)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.searchCovers(bookTitle, isbn); showCoverDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("표지 검색")
                    }
                    OutlinedTextField(
                        value = coverImageUrl,
                        onValueChange = { coverImageUrl = it },
                        label = { Text("표지 URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            OutlinedTextField(
                value = bookTitle,
                onValueChange = { bookTitle = it; titleError = false },
                label = { Text("제목 *") },
                isError = titleError,
                supportingText = if (titleError) {{ Text("제목을 입력해주세요") }} else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = author,
                onValueChange = { author = it; authorError = false },
                label = { Text("저자 *") },
                isError = authorError,
                supportingText = if (authorError) {{ Text("저자를 입력해주세요") }} else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = publisher,
                onValueChange = { publisher = it },
                label = { Text("출판사") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = totalPages,
                    onValueChange = { totalPages = it.filter { c -> c.isDigit() } },
                    label = { Text("총 페이지") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = publishYear,
                    onValueChange = { publishYear = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("출판년도") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            ExposedDropdownMenuBox(
                expanded = genreExpanded,
                onExpandedChange = { genreExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedGenre.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("장르") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = genreExpanded,
                    onDismissRequest = { genreExpanded = false }
                ) {
                    BookGenre.values().forEach { genre ->
                        DropdownMenuItem(
                            text = { Text(genre.label) },
                            onClick = {
                                selectedGenre = genre
                                genreExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedStatus.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("독서 상태") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    ReadingStatus.values().forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.label) },
                            onClick = {
                                selectedStatus = status
                                statusExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = isbn,
                onValueChange = { isbn = it.filter { c -> c.isDigit() }.take(13) },
                label = { Text("ISBN (선택)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CoverPickerDialog(
    candidates: List<String>,
    loading: Boolean,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    debug: String = ""
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("표지 선택") },
        text = {
            Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    loading -> CircularProgressIndicator()
                    candidates.isEmpty() -> Text("표지를 찾지 못했어요. URL을 직접 입력해 주세요.")
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(candidates) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "표지 후보",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .aspectRatio(0.7f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onPick(url) }
                            )
                        }
                    }
                }
            }
            if (debug.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = debug,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}
