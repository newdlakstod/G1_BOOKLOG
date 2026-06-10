package com.g1.booklog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.ReadingStatus
import com.g1.booklog.ui.viewmodel.BookViewModel
import java.util.*

private val DAY_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: BookViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val now = Calendar.getInstance()
    var displayYear  by remember { mutableIntStateOf(now.get(Calendar.YEAR))  }
    var displayMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }

    val todayYear  = now.get(Calendar.YEAR)
    val todayMonth = now.get(Calendar.MONTH)
    val todayDay   = now.get(Calendar.DAY_OF_MONTH)

    var selectedDayBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    // day -> completed books with endDate in this month
    val completedOnDay: Map<Int, List<Book>> = remember(uiState.allBooks, displayYear, displayMonth) {
        uiState.allBooks
            .filter { book ->
                book.status == ReadingStatus.COMPLETED &&
                book.endDate != null &&
                Calendar.getInstance().apply { timeInMillis = book.endDate }.let {
                    it.get(Calendar.YEAR) == displayYear &&
                    it.get(Calendar.MONTH) == displayMonth
                }
            }
            .groupBy { book ->
                Calendar.getInstance().apply { timeInMillis = book.endDate!! }
                    .get(Calendar.DAY_OF_MONTH)
            }
    }

    val firstDayCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, displayYear)
        set(Calendar.MONTH, displayMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth    = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1  // 0=Sun…6=Sat

    val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
    val weeks = List(totalCells) { i ->
        val day = i - firstDayOfWeek + 1
        if (day in 1..daysInMonth) day else null
    }.chunked(7)

    selectedDay?.let { day ->
        AlertDialog(
            onDismissRequest = { selectedDay = null },
            title = { Text("${displayYear}년 ${displayMonth + 1}월 ${day}일") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    selectedDayBooks.forEach { book ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (book.coverImageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = book.coverImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(32.dp, 46.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                BookCoverPlaceholder(
                                    title = book.title,
                                    modifier = Modifier.size(32.dp, 46.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    book.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (book.author.isNotBlank()) {
                                    Text(
                                        book.author,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDay = null }) { Text("닫기") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "달력",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── 월 네비게이션 ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    if (displayMonth == 0) { displayMonth = 11; displayYear-- }
                    else displayMonth--
                }) {
                    Icon(Icons.Default.KeyboardArrowLeft, "이전 달")
                }
                Text(
                    text = "${displayYear}년 ${displayMonth + 1}월",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    if (displayMonth == 11) { displayMonth = 0; displayYear++ }
                    else displayMonth++
                }) {
                    Icon(Icons.Default.KeyboardArrowRight, "다음 달")
                }
            }

            // ── 요일 헤더 ────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                DAY_LABELS.forEachIndexed { idx, label ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = when (idx) {
                            0    -> MaterialTheme.colorScheme.error
                            6    -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp)

            // ── 달력 그리드 ──────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                weeks.forEachIndexed { weekIdx, week ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        week.forEachIndexed { dayIdx, day ->
                            val dayBooks = day?.let { completedOnDay[it] } ?: emptyList()
                            DayCell(
                                day       = day,
                                books     = dayBooks,
                                isToday   = day != null
                                            && displayYear  == todayYear
                                            && displayMonth == todayMonth
                                            && day == todayDay,
                                isSunday   = dayIdx == 0,
                                isSaturday = dayIdx == 6,
                                onClick    = if (dayBooks.isNotEmpty() && day != null) {
                                    { selectedDay = day; selectedDayBooks = dayBooks }
                                } else null,
                                modifier   = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                    if (weekIdx < weeks.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int?,
    books: List<Book>,
    isToday: Boolean,
    isSunday: Boolean,
    isSaturday: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(1.5.dp).then(
        if (onClick != null) Modifier.clickable { onClick() } else Modifier
    )) {
        if (day != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 날짜 숫자
                Box(
                    modifier = if (isToday)
                        Modifier.size(20.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    else
                        Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.toString(),
                        fontSize = 10.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isToday    -> MaterialTheme.colorScheme.onPrimary
                            isSunday   -> MaterialTheme.colorScheme.error
                            isSaturday -> MaterialTheme.colorScheme.primary
                            else       -> MaterialTheme.colorScheme.onSurface
                        },
                        textAlign = TextAlign.Center
                    )
                }

                // 책 표지
                if (books.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        val book = books.first()
                        if (book.coverImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = book.coverImageUrl,
                                contentDescription = book.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            BookCoverPlaceholder(
                                title = book.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // 같은 날 2권 이상이면 뱃지 표시
                        if (books.size > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${books.size - 1}",
                                    fontSize = 6.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}
