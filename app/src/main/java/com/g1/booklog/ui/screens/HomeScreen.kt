package com.g1.booklog.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.ReadingStatus
import com.g1.booklog.ui.theme.StarColor
import com.g1.booklog.ui.theme.StatusCompleted
import com.g1.booklog.ui.theme.StatusReading
import com.g1.booklog.ui.theme.StatusWantToRead
import com.g1.booklog.ui.viewmodel.BookViewModel
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BookViewModel,
    onBookClick: (Long) -> Unit,
    onAddBook: () -> Unit,
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var selectedStatus by remember { mutableStateOf(ReadingStatus.READING) }

    val displayedBooks = remember(uiState.allBooks, selectedStatus) {
        uiState.allBooks
            .filter { it.status == selectedStatus }
            .sortedByDescending { it.updatedAt }
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("책 삭제") },
            text = { Text("\"${book.title}\"를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBook(book)
                    bookToDelete = null
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "지독한책장",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (darkTheme) "라이트 모드" else "다크 모드",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "프로필",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
                .padding(paddingValues),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (displayedBooks.isNotEmpty()) {
                    ReadingPagerCarousel(
                        books = displayedBooks,
                        onBookClick = onBookClick,
                        onBookLongClick = { bookToDelete = it }
                    )
                } else {
                    EmptyReadingState(status = selectedStatus, isFirstBook = uiState.allBooks.isEmpty(), onAddBook = onAddBook)
                }
            }

            StatusPillsRow(
                wantToRead = uiState.wantToReadCount,
                reading = uiState.readingCount,
                completed = uiState.completedCount,
                selectedStatus = selectedStatus,
                onStatusSelect = { selectedStatus = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadingPagerCarousel(
    books: List<Book>,
    onBookClick: (Long) -> Unit,
    onBookLongClick: (Book) -> Unit
) {
    val listState = rememberLazyListState()
    val snapFling = rememberSnapFlingBehavior(listState)

    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - viewCenter) }
                ?.index ?: 0
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val itemWidth = maxWidth - 160.dp
            LazyRow(
                state = listState,
                flingBehavior = snapFling,
                contentPadding = PaddingValues(horizontal = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(books, key = { _, book -> book.id }) { index, book ->
                    val scale by remember {
                        derivedStateOf {
                            val info = listState.layoutInfo
                            val viewCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                            if (item != null) {
                                val itemCenter = item.offset + item.size / 2f
                                val halfViewport = (info.viewportEndOffset - info.viewportStartOffset) / 2f
                                val fraction = (kotlin.math.abs(itemCenter - viewCenter) / halfViewport).coerceIn(0f, 1f)
                                lerp(1.0f, 0.8f, fraction)
                            } else 0.8f
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .scale(scale)
                            .shadow(elevation = 16.dp, shape = RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = { onBookClick(book.id) },
                                onLongClick = { onBookLongClick(book) }
                            )
                    ) {
                        if (book.coverImageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = book.coverImageUrl,
                                contentDescription = book.title,
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.67f),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            BookCoverPlaceholder(
                                title = book.title,
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.67f)
                            )
                        }
                        if (book.totalPages != null && book.totalPages > 0) {
                            val progress = (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                                        )
                                    )
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = books.getOrNull(centeredIndex)?.title ?: "",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = books.getOrNull(centeredIndex)?.author ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )

        if (books.size > 1) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(books.size) { index ->
                    val isSelected = centeredIndex == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyReadingState(status: ReadingStatus, isFirstBook: Boolean, onAddBook: () -> Unit) {
    if (isFirstBook) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "지독한책장에 오신 걸 환영해요",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "첫 번째 책을 추가하고\n나만의 독서 기록을 시작해보세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddBook,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("첫 책 추가하기")
            }
        }
    } else {
        val message = when (status) {
            ReadingStatus.WANT_TO_READ -> "독서예정 책이 없어요"
            ReadingStatus.READING      -> "독서중인 책이 없어요"
            ReadingStatus.COMPLETED    -> "완독한 책이 없어요"
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onAddBook) { Text("책 추가하기") }
        }
    }
}

@Composable
private fun StatusPillsRow(
    wantToRead: Int,
    reading: Int,
    completed: Int,
    selectedStatus: ReadingStatus,
    onStatusSelect: (ReadingStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        StatusCountPill(
            label = "독서예정", count = wantToRead, color = StatusWantToRead,
            selected = selectedStatus == ReadingStatus.WANT_TO_READ,
            onClick = { onStatusSelect(ReadingStatus.WANT_TO_READ) }
        )
        StatusCountPill(
            label = "독서중", count = reading, color = StatusReading,
            selected = selectedStatus == ReadingStatus.READING,
            onClick = { onStatusSelect(ReadingStatus.READING) }
        )
        StatusCountPill(
            label = "독서완료", count = completed, color = StatusCompleted,
            selected = selectedStatus == ReadingStatus.COMPLETED,
            onClick = { onStatusSelect(ReadingStatus.COMPLETED) }
        )
    }
}

@Composable
private fun StatusCountPill(
    label: String,
    count: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else color.copy(alpha = 0.13f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else color
        )
    }
}

// ── 공유 컴포저블 (LibraryScreen 등에서 사용) ────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookGridItem(
    book: Book,
    showStatus: Boolean = true,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
        ) {
            if (book.coverImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = book.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                BookCoverPlaceholder(
                    title = book.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(8.dp))
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (showStatus) {
            Spacer(Modifier.height(2.dp))
            StatusPill(status = book.status)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (book.coverImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = book.coverImageUrl,
                    contentDescription = book.title,
                    modifier = Modifier
                        .size(60.dp, 84.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                BookCoverPlaceholder(
                    title = book.title,
                    modifier = Modifier
                        .size(60.dp, 84.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(6.dp))
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(text = book.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(status = book.status)
                    if (book.status == ReadingStatus.READING && book.totalPages != null && book.totalPages > 0) {
                        Text(text = "${book.currentPage}/${book.totalPages}p", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                if (book.status == ReadingStatus.COMPLETED && book.rating > 0f) StarRatingDisplay(rating = book.rating)
                if (book.status == ReadingStatus.READING && book.totalPages != null && book.totalPages > 0) {
                    LinearProgressIndicator(
                        progress = { (book.currentPage.toFloat() / book.totalPages).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = StatusReading
                    )
                }
            }
        }
    }
}

@Composable
fun BookCoverPlaceholder(title: String, modifier: Modifier = Modifier) {
    val colors = listOf(
        Color(0xFF44A194), Color(0xFF537D96), Color(0xFF6DBDB4),
        Color(0xFF3D5F74), Color(0xFF7A9E97), Color(0xFFEC8F8D)
    )
    val color = colors[title.hashCode().and(0x7FFFFFFF) % colors.size]
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(color), contentAlignment = Alignment.Center) {
        Text(text = title.take(1), style = MaterialTheme.typography.headlineMedium, color = Color.White)
    }
}

@Composable
fun StatusPill(status: ReadingStatus) {
    val (color, label) = when (status) {
        ReadingStatus.WANT_TO_READ -> StatusWantToRead to status.label
        ReadingStatus.READING      -> StatusReading    to status.label
        ReadingStatus.COMPLETED    -> StatusCompleted  to status.label
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 9.sp, color = color, lineHeight = 12.sp)
    }
}

@Composable
fun StatusBadge(status: ReadingStatus) = StatusPill(status = status)

@Composable
fun StarRatingDisplay(rating: Float, maxStars: Int = 5, starSize: androidx.compose.ui.unit.Dp = 14.dp) {
    Row {
        repeat(maxStars) { index ->
            Icon(
                imageVector = when {
                    index < rating.toInt() -> Icons.Default.Star
                    index < rating         -> Icons.Default.StarHalf
                    else                   -> Icons.Default.StarOutline
                },
                contentDescription = null,
                tint = StarColor,
                modifier = Modifier.size(starSize)
            )
        }
    }
}
