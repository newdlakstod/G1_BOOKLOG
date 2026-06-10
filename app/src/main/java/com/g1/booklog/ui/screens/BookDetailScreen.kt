package com.g1.booklog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.ReadingStatus
import com.g1.booklog.ui.theme.StarColor
import com.g1.booklog.ui.viewmodel.BookViewModel
import java.text.SimpleDateFormat
import java.util.*

// 메모 항목 구분자 (비인쇄 문자 — 사용자 입력 불가)
private const val ENTRY_SEP = ""
private const val DATE_SEP  = ""

private data class MemoEntry(val date: String, val content: String)

private fun parseMemoEntries(raw: String): List<MemoEntry> {
    if (raw.isBlank()) return emptyList()
    return raw.split(ENTRY_SEP).filter { it.isNotBlank() }.map { part ->
        val idx = part.indexOf(DATE_SEP)
        if (idx >= 0) MemoEntry(part.substring(0, idx), part.substring(idx + 1))
        else MemoEntry("", part)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: Long,
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit,
    onEditBook: (Long) -> Unit = {}
) {
    val bookFromDb by viewModel.getBookById(bookId).collectAsState(initial = null)

    // 드래프트: DB에서 처음 받아온 값으로 초기화하고, 이후 DB 변경 무시
    var draft by remember { mutableStateOf<Book?>(null) }
    if (draft == null && bookFromDb != null) {
        draft = bookFromDb
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val book = draft ?: return

    // 별점/한줄평/메모는 별도 로컬 상태로 관리 (즉시 저장 X)
    var reviewText by remember(book.id) { mutableStateOf(book.review) }
    var memoEntries by remember(book.id) {
        mutableStateOf(parseMemoEntries(book.memo).map { it.content }.filter { it.isNotBlank() })
    }
    var highlightEntries by remember(book.id) {
        mutableStateOf(book.highlights.split(ENTRY_SEP).filter { it.isNotBlank() })
    }

    fun saveAndBack() {
        val encodedMemo = memoEntries.filter { it.isNotBlank() }.joinToString(ENTRY_SEP)
        val encodedHighlights = highlightEntries.filter { it.isNotBlank() }.joinToString(ENTRY_SEP)
        viewModel.updateBook(book.copy(review = reviewText, memo = encodedMemo, highlights = encodedHighlights))
        scope.launch {
            snackbarHostState.showSnackbar("저장됐습니다")
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { BookLogSnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(book.title, maxLines = 1, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    IconButton(onClick = { saveAndBack() }) {
                        Icon(Icons.Default.Check, "수정완료", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            BookHeader(book = book)

            StatusSection(
                book = book,
                onStatusChange = { status ->
                    val now = System.currentTimeMillis()
                    draft = book.copy(
                        status = status,
                        startDate = if (status == ReadingStatus.READING && book.startDate == null) now else book.startDate,
                        endDate = when (status) {
                            ReadingStatus.COMPLETED -> book.endDate ?: now
                            ReadingStatus.READING -> null
                            else -> book.endDate
                        }
                    )
                },
                onEndDateChange = { millis ->
                    val newStatus = if (millis != null && millis <= todayEndMillis() &&
                        book.status != ReadingStatus.COMPLETED) ReadingStatus.COMPLETED
                    else book.status
                    draft = book.copy(endDate = millis, status = newStatus)
                }
            )

            RatingReviewSection(
                book = book,
                reviewText = reviewText,
                onReviewTextChange = { reviewText = it },
                onRatingChange = { draft = book.copy(rating = it) }
            )

            MemoSection(
                entries = memoEntries,
                onEntriesChange = { memoEntries = it }
            )

            HighlightSection(
                entries = highlightEntries,
                onEntriesChange = { highlightEntries = it }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun todayEndMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}.timeInMillis

@Composable
private fun BookHeader(book: Book) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (book.coverImageUrl.isNotEmpty()) {
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .size(100.dp, 140.dp)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            BookCoverPlaceholder(
                title = book.title,
                modifier = Modifier
                    .size(100.dp, 140.dp)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(10.dp))
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = book.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (book.publisher.isNotEmpty()) {
                Text(
                    text = book.publisher,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (book.publishYear != null) {
                Text(
                    text = "${book.publishYear}년 출판",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text(
                text = book.genre.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusSection(
    book: Book,
    onStatusChange: (ReadingStatus) -> Unit,
    onEndDateChange: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)

    val endPickerState = rememberDatePickerState(
        initialSelectedDateMillis = book.endDate ?: System.currentTimeMillis()
    )

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onEndDateChange(endPickerState.selectedDateMillis)
                    showEndDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("취소") }
            }
        ) { DatePicker(state = endPickerState) }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("독서 상태", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = book.status.label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ReadingStatus.values().forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status.label) },
                        onClick = {
                            if (book.status != status) onStatusChange(status)
                            expanded = false
                        }
                    )
                }
            }
        }

        if (book.status == ReadingStatus.READING || book.status == ReadingStatus.COMPLETED) {
            Spacer(modifier = Modifier.height(8.dp))
            DateRow(
                label = "독서 완료일",
                dateText = book.endDate?.let { dateFormat.format(Date(it)) },
                onClick = { showEndDatePicker = true },
                onClear = {
                    onEndDateChange(null)
                    if (book.status == ReadingStatus.COMPLETED) onStatusChange(ReadingStatus.READING)
                }
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun DateRow(
    label: String,
    dateText: String?,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CalendarToday, null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dateText ?: "날짜 설정",
                style = MaterialTheme.typography.bodyMedium,
                color = if (dateText != null) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.primary
            )
            if (dateText != null && onClear != null) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

// ── 별점 & 한줄평 ─────────────────────────────────────────────────────────

@Composable
private fun RatingReviewSection(
    book: Book,
    reviewText: String,
    onReviewTextChange: (String) -> Unit,
    onRatingChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("별점 & 한줄평", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(5) { index ->
                val starValue = (index + 1).toFloat()
                Icon(
                    imageVector = if (index < book.rating) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = null,
                    tint = StarColor,
                    modifier = Modifier
                        .size(34.dp)
                        .clickable {
                            onRatingChange(if (book.rating == starValue) 0f else starValue)
                        }
                )
            }
            if (book.rating > 0f) {
                Text(
                    text = String.format("%.1f", book.rating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                IconButton(onClick = { onRatingChange(0f) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close, "별점 취소",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = reviewText,
            onValueChange = onReviewTextChange,
            label = { Text("한줄평") },
            placeholder = { Text("한줄평을 남겨보세요") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )
    }
    HorizontalDivider()
}

// ── 메모 (항목 추가형) ────────────────────────────────────────────────────

@Composable
private fun MemoSection(
    entries: List<String>,
    onEntriesChange: (List<String>) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("메모", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { onEntriesChange(entries + "") }) {
                Icon(Icons.Default.Add, "메모 추가", modifier = Modifier.size(20.dp))
            }
        }

        if (entries.isEmpty()) {
            TextButton(
                onClick = { onEntriesChange(listOf("")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("첫 메모를 남겨보세요")
            }
        } else {
            entries.forEachIndexed { index, content ->
                Spacer(modifier = Modifier.height(8.dp))
                MemoEntryCard(
                    content = content,
                    onContentChange = { newContent ->
                        onEntriesChange(entries.toMutableList().also { it[index] = newContent })
                    },
                    onDelete = {
                        onEntriesChange(entries.toMutableList().also { it.removeAt(index) })
                    }
                )
            }
        }
    }
    HorizontalDivider()
}

// ── 필사 (하이라이트) ─────────────────────────────────────────────────────

@Composable
private fun HighlightSection(
    entries: List<String>,
    onEntriesChange: (List<String>) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("필사", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { onEntriesChange(entries + "") }) {
                Icon(Icons.Default.Add, "필사 추가", modifier = Modifier.size(20.dp))
            }
        }

        if (entries.isEmpty()) {
            TextButton(
                onClick = { onEntriesChange(listOf("")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("기억하고 싶은 구절을 남겨보세요")
            }
        } else {
            entries.forEachIndexed { index, content ->
                Spacer(modifier = Modifier.height(8.dp))
                HighlightEntryCard(
                    content = content,
                    onContentChange = { newContent ->
                        onEntriesChange(entries.toMutableList().also { it[index] = newContent })
                    },
                    onDelete = {
                        onEntriesChange(entries.toMutableList().also { it.removeAt(index) })
                    }
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun HighlightEntryCard(
    content: String,
    onContentChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val accentColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentColor)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (content.isEmpty()) {
                        Text("기억하고 싶은 구절을 입력하세요", style = MaterialTheme.typography.bodyMedium, color = hintColor)
                    }
                    inner()
                }
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close, "삭제",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun MemoEntryCard(
    content: String,
    onContentChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        BasicTextField(
            value = content,
            onValueChange = onContentChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = textColor,
                lineHeight = 22.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (content.isEmpty()) {
                    Text("내용을 입력하세요", style = MaterialTheme.typography.bodyMedium, color = hintColor)
                }
                inner()
            }
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Close, "삭제",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}
