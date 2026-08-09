package com.g1.booklog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.g1.booklog.data.model.ReadingStatus
import com.g1.booklog.data.network.GoogleBookItem
import com.g1.booklog.ui.viewmodel.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchScreen(
    viewModel: BookViewModel,
    onBookSelected: () -> Unit,
    onNavigateBack: () -> Unit,
    onAddManually: () -> Unit = {}
) {
    val results by viewModel.naverResults.collectAsState()
    val isSearching by viewModel.naverSearching.collectAsState()
    val error by viewModel.naverError.collectAsState()
    var query by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    var pendingItem by remember { mutableStateOf<GoogleBookItem?>(null) }

    // 독서 상태 선택 다이얼로그
    pendingItem?.let { item ->
        StatusPickerDialog(
            item = item,
            viewModel = viewModel,
            onConfirm = { status, cover ->
                viewModel.addBookFromSearch(item, status, cover)
                viewModel.clearNaverSearch()
                pendingItem = null
                onBookSelected()
            },
            onDismiss = { pendingItem = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("책 검색") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearNaverSearch()
                        onNavigateBack()
                    }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 검색바
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("책 제목 또는 저자 검색", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.searchNaverBooks(query)
                                hasSearched = true
                            },
                            enabled = query.isNotBlank() && !isSearching
                        ) {
                            Icon(Icons.Default.Search, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        viewModel.searchNaverBooks(query)
                        hasSearched = true
                    }),
                    shape = RoundedCornerShape(50.dp)
                )
            }

            // 로딩
            if (isSearching) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // 네트워크 오류만 표시 (결과 없음 메시지는 아래 empty state에서 처리)
            val errorMsg = error
            if (!isSearching && errorMsg != null && errorMsg.startsWith("검색 오류")) {
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 검색 결과 없음 + 직접 추가
            if (!isSearching && hasSearched && results.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "검색 결과가 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Button(
                        onClick = onAddManually,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("직접 추가하기")
                    }
                }
            }

            // 검색 결과 (화면 폭에 맞춰 열 개수 자동 조절)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(100.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { item ->
                    BookGridCard(
                        item = item,
                        onClick = { pendingItem = item }
                    )
                }
                item(span = { GridItemSpan(3) }) {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusPickerDialog(
    item: GoogleBookItem,
    viewModel: BookViewModel,
    onConfirm: (ReadingStatus, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember { mutableStateOf(ReadingStatus.WANT_TO_READ) }
    var coverUrl by remember { mutableStateOf(item.getThumbnail()) }

    var showCoverDialog by remember { mutableStateOf(false) }
    val coverCandidates by viewModel.coverCandidates.collectAsState()
    val coverSearching by viewModel.coverSearching.collectAsState()
    val coverDebug by viewModel.coverDebug.collectAsState()
    if (showCoverDialog) {
        CoverPickerDialog(
            candidates = coverCandidates,
            loading = coverSearching,
            onPick = { coverUrl = it; showCoverDialog = false; viewModel.clearCoverCandidates() },
            onDismiss = { showCoverDialog = false; viewModel.clearCoverCandidates() },
            debug = coverDebug
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = item.getTitle(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 표지가 없을 때만 표지 추가 UI 노출 (있으면 그대로 둠)
                if (coverUrl.isBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BookCoverPlaceholder(
                            title = item.getTitle(),
                            modifier = Modifier
                                .width(48.dp)
                                .aspectRatio(0.7f)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        OutlinedButton(onClick = {
                            viewModel.searchCovers(item.getTitle(), item.getIsbn13())
                            showCoverDialog = true
                        }) {
                            Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("표지 검색")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = "독서 상태를 선택해주세요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                ReadingStatus.values().forEach { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStatus = status }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(status.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedStatus, coverUrl) }) {
                Text("추가")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun BookGridCard(item: GoogleBookItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        val thumbnail = item.getThumbnail()
        if (thumbnail.isNotEmpty()) {
            AsyncImage(
                model = thumbnail,
                contentDescription = item.getTitle(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            BookCoverPlaceholder(
                title = item.getTitle(),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = item.getTitle(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = item.getAuthor(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
