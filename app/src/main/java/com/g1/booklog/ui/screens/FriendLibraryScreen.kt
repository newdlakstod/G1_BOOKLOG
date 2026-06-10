package com.g1.booklog.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.g1.booklog.ui.theme.AccentPeach
import com.g1.booklog.ui.theme.Secondary as NavyColor
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.ReadingStatus
import com.g1.booklog.ui.viewmodel.BookViewModel
import com.g1.booklog.ui.viewmodel.FriendViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendLibraryScreen(
    friendViewModel: FriendViewModel,
    bookViewModel: BookViewModel,
    onNavigateBack: () -> Unit
) {
    val friend by friendViewModel.selectedFriend.collectAsState()
    val allFriendBooks by friendViewModel.friendBooks.collectAsState()
    val myUiState by bookViewModel.uiState.collectAsState()
    val myUser = FirebaseAuth.getInstance().currentUser
    val myNickname by bookViewModel.myDisplayName.collectAsState()
    val myCustomPhotoUrl by bookViewModel.myPhotoUrl.collectAsState()

    val completedBooks = remember(allFriendBooks) {
        allFriendBooks.filter { it.status == ReadingStatus.COMPLETED }
    }
    val myCompletedCount = myUiState.completedCount
    val friendCompletedCount = completedBooks.size

    LaunchedEffect(friend?.uid) {
        friend?.uid?.let { friendViewModel.loadFriendBooks(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${friend?.visibleName() ?: ""}의 서재") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (completedBooks.isEmpty() && allFriendBooks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "완독한 책이 없습니다",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 인포그래픽
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    ReadingComparisonCard(
                        myName = myNickname.ifBlank { myUser?.displayName ?: "나" },
                        myPhotoUrl = myCustomPhotoUrl.ifEmpty { myUser?.photoUrl?.toString() ?: "" },
                        myCount = myCompletedCount,
                        friendName = friend?.visibleName() ?: "",
                        friendPhotoUrl = friend?.photoUrl ?: "",
                        friendCount = friendCompletedCount,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (completedBooks.isEmpty()) {
                    item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                        Text(
                            text = "완독한 책이 없습니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    items(completedBooks) { book ->
                        FriendBookCard(book = book)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingComparisonCard(
    myName: String,
    myPhotoUrl: String,
    myCount: Int,
    friendName: String,
    friendPhotoUrl: String,
    friendCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (myCount + friendCount).coerceAtLeast(1)
    val myFraction = myCount.toFloat() / total
    val friendFraction = friendCount.toFloat() / total

    // 이기는 쪽 = AccentPeach(붉은 톤), 지는 쪽 = Navy, 동점 = 둘 다 Primary
    val myWinning = myCount > friendCount
    val friendWinning = friendCount > myCount
    val myMainColor = when {
        myWinning -> AccentPeach
        friendWinning -> NavyColor
        else -> MaterialTheme.colorScheme.primary
    }
    val friendMainColor = when {
        friendWinning -> AccentPeach
        myWinning -> NavyColor
        else -> MaterialTheme.colorScheme.secondary
    }

    var animStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animStarted = true }
    val myBar by animateFloatAsState(
        targetValue = if (animStarted) myFraction else 0f,
        animationSpec = tween(800),
        label = "myBar"
    )
    val friendBar by animateFloatAsState(
        targetValue = if (animStarted) friendFraction else 0f,
        animationSpec = tween(800),
        label = "friendBar"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 나
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(width = 44.dp, height = 56.dp)) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            FriendAvatar(name = myName, photoUrl = myPhotoUrl, size = 44.dp)
                        }
                        if (myWinning) {
                            Text("👑", fontSize = 14.sp, modifier = Modifier.align(Alignment.TopCenter))
                        }
                    }
                    Text(
                        text = myName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${myCount}권",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = myMainColor,
                        fontSize = 20.sp
                    )
                }

                // VS
                Text(
                    text = "VS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )

                // 친구
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(width = 44.dp, height = 56.dp)) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            FriendAvatar(name = friendName, photoUrl = friendPhotoUrl, size = 44.dp)
                        }
                        if (friendWinning) {
                            Text("👑", fontSize = 14.sp, modifier = Modifier.align(Alignment.TopCenter))
                        }
                    }
                    Text(
                        text = friendName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${friendCount}권",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = friendMainColor,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 합산 바 차트
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (myBar > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(myBar)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(myMainColor, myMainColor.copy(alpha = 0.75f))
                                )
                            )
                    )
                }
                if (friendBar > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(friendBar)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(friendMainColor.copy(alpha = 0.75f), friendMainColor)
                                )
                            )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${myCount}권",
                    style = MaterialTheme.typography.labelSmall,
                    color = myMainColor
                )
                Text(
                    text = "${friendCount}권",
                    style = MaterialTheme.typography.labelSmall,
                    color = friendMainColor
                )
            }
        }
    }
}

@Composable
private fun FriendBookCard(book: Book) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (book.coverImageUrl.isNotEmpty()) {
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            BookCoverPlaceholder(
                title = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = book.author,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
