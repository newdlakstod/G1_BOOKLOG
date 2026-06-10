package com.g1.booklog.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.ReadingStatus
import com.g1.booklog.ui.theme.StatusCompleted
import com.g1.booklog.ui.theme.StatusReading
import com.g1.booklog.ui.theme.StatusWantToRead
import com.g1.booklog.ui.viewmodel.BookViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val monthlyStats by viewModel.getMonthlyReadingStats().collectAsState(initial = emptyList())

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("booklog_prefs", Context.MODE_PRIVATE) }
    var annualGoal by remember { mutableIntStateOf(prefs.getInt("annual_goal", 12)) }

    val thisYear = Calendar.getInstance().get(Calendar.YEAR)
    val completedThisYear = remember(uiState.allBooks, thisYear) {
        uiState.allBooks.count { book ->
            book.status == ReadingStatus.COMPLETED &&
            book.endDate != null &&
            Calendar.getInstance().apply { timeInMillis = book.endDate }
                .get(Calendar.YEAR) == thisYear
        }
    }

    val recentCompleted = remember(uiState.allBooks) {
        uiState.allBooks
            .filter { it.status == ReadingStatus.COMPLETED && it.endDate != null }
            .sortedByDescending { it.endDate }
            .take(10)
    }

    val ratingStats = remember(uiState.allBooks) {
        val rated = uiState.allBooks.filter { it.status == ReadingStatus.COMPLETED && it.rating > 0f }
        (5 downTo 1).map { stars -> stars to rated.count { it.rating.toInt() == stars } }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "독서 통계",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusSummaryCard(
                wantToRead = uiState.wantToReadCount,
                reading = uiState.readingCount,
                completed = uiState.completedCount
            )

            AnnualGoalCard(
                year = thisYear,
                completedThisYear = completedThisYear,
                goal = annualGoal,
                onGoalChange = { newGoal ->
                    annualGoal = newGoal
                    prefs.edit().putInt("annual_goal", newGoal).apply()
                }
            )

            if (monthlyStats.isNotEmpty()) {
                MonthlyBarCard(stats = monthlyStats)
            }

            RatingDistributionCard(stats = ratingStats)

            if (recentCompleted.isNotEmpty()) {
                RecentCompletedCard(books = recentCompleted)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── 전체 현황 ─────────────────────────────────────────────────────────────

@Composable
private fun StatusSummaryCard(wantToRead: Int, reading: Int, completed: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusStatItem(count = wantToRead, label = "독서예정", color = StatusWantToRead)
            StatDivider()
            StatusStatItem(count = reading,    label = "독서중",   color = StatusReading)
            StatDivider()
            StatusStatItem(count = completed,  label = "독서완료", color = StatusCompleted)
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    )
}

@Composable
private fun StatusStatItem(count: Int, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = count.toString(),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            lineHeight = 32.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )
    }
}

// ── 연간 목표 ─────────────────────────────────────────────────────────────

@Composable
private fun AnnualGoalCard(
    year: Int,
    completedThisYear: Int,
    goal: Int,
    onGoalChange: (Int) -> Unit
) {
    val rate = if (goal > 0) (completedThisYear.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val percent = (rate * 100).toInt()
    var goalText by remember(goal) { mutableStateOf(goal.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${year}년 독서 목표",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (goal > 1) {
                                onGoalChange(goal - 1)
                                goalText = (goal - 1).toString()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { text ->
                            goalText = text.filter { it.isDigit() }.take(3)
                            val parsed = goalText.toIntOrNull()
                            if (parsed != null && parsed >= 1) onGoalChange(parsed)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        suffix = { Text("권") },
                        modifier = Modifier.width(80.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    IconButton(
                        onClick = {
                            onGoalChange(goal + 1)
                            goalText = (goal + 1).toString()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${completedThisYear}권 달성",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${percent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(rate)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "목표까지 ${(goal - completedThisYear).coerceAtLeast(0)}권 남았어요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

// ── 월별 롤리팝 차트 (1월~12월) ──────────────────────────────────────────

@Composable
private fun MonthlyBarCard(stats: List<Pair<String, Int>>) {
    val maxCount = stats.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val accentColor = Color(0xFF537D96)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "월별 독서량",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.forEachIndexed { idx, (_, count) ->
                    val isCurrentMonth = (idx + 1) == currentMonth
                    val fraction = if (count > 0) count.toFloat() / maxCount else 0f
                    val circleSize = if (count > 0) {
                        (7 + fraction * 7).dp.coerceIn(7.dp, 14.dp)
                    } else 5.dp
                    val stemHeight = (fraction * 60).dp
                    val dotColor = if (count > 0) {
                        if (isCurrentMonth) accentColor else accentColor.copy(alpha = 0.5f)
                    } else accentColor.copy(alpha = 0.13f)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Circle with count inside
                        Box(
                            modifier = Modifier
                                .size(circleSize)
                                .clip(CircleShape)
                                .background(dotColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (count > 0) {
                                Text(
                                    text = count.toString(),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                        // Stem
                        if (stemHeight > 0.dp) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .height(stemHeight)
                                    .background(dotColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // Month label
                        Text(
                            text = "${idx + 1}",
                            fontSize = 9.sp,
                            fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrentMonth) accentColor
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

// ── 별점 분포 ─────────────────────────────────────────────────────────────

@Composable
private fun RatingDistributionCard(stats: List<Pair<Int, Int>>) {
    val maxCount = stats.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val starColor = Color(0xFFEC8F8D)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "별점 분포",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            stats.forEach { (stars, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "★".repeat(stars),
                        fontSize = 10.sp,
                        color = starColor,
                        modifier = Modifier.width(52.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(starColor.copy(alpha = 0.12f))
                    ) {
                        val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(starColor)
                        )
                    }
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.width(20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

// ── 최근 완독 리스트 ───────────────────────────────────────────────────────

@Composable
private fun RecentCompletedCard(books: List<Book>) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.KOREA) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "최근 완독",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            books.forEachIndexed { index, book ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 책 표지 썸네일
                    if (book.coverImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = book.coverImageUrl,
                            contentDescription = book.title,
                            modifier = Modifier
                                .size(34.dp, 48.dp)
                                .shadow(3.dp, RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        BookCoverPlaceholder(
                            title = book.title,
                            modifier = Modifier
                                .size(34.dp, 48.dp)
                                .shadow(3.dp, RoundedCornerShape(4.dp))
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (book.author.isNotBlank()) {
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (book.rating > 0f) {
                            StarRatingDisplay(rating = book.rating, starSize = 12.dp)
                        }
                    }

                    book.endDate?.let { date ->
                        Text(
                            text = dateFormat.format(Date(date)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 10.sp
                        )
                    }
                }

                if (index < books.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}
