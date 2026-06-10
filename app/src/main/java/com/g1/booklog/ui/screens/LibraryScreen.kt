package com.g1.booklog.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.g1.booklog.data.model.Book
import com.g1.booklog.data.model.ReadingStatus
import com.g1.booklog.ui.viewmodel.BookViewModel

enum class SortOrder(val label: String) {
    ADDED("추가순"),
    TITLE("책제목 가나다순"),
    RATING("별점순"),
    DATE_COMPLETE_ASC("독서완료일 오름차순"),
    DATE_COMPLETE_DESC("독서완료일 내림차순")
}

private fun List<Book>.applySortOrder(order: SortOrder): List<Book> = when (order) {
    SortOrder.ADDED -> this
    SortOrder.TITLE -> sortedBy { it.title }
    SortOrder.RATING -> sortedByDescending { it.rating }
    SortOrder.DATE_COMPLETE_ASC -> sortedBy { it.endDate ?: Long.MAX_VALUE }
    SortOrder.DATE_COMPLETE_DESC -> sortedByDescending { it.endDate ?: Long.MIN_VALUE }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: BookViewModel,
    onBookClick: (Long) -> Unit,
    onAddBook: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("전체", "독서예정", "독서중", "독서완료")

    var selectedTab by remember { mutableStateOf(0) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    var sortOrder by remember { mutableStateOf(SortOrder.ADDED) }
    var showSortMenu by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStatusPicker by remember { mutableStateOf(false) }
    var pendingTabChange by remember { mutableStateOf<Int?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Pager for swipe-to-change-tab
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) pagerState.animateScrollToPage(selectedTab)
    }
    LaunchedEffect(pagerState.settledPage) {
        if (selectedTab != pagerState.settledPage) {
            if (isSelectionMode) {
                pendingTabChange = pagerState.settledPage
                pagerState.scrollToPage(selectedTab)
            } else {
                selectedTab = pagerState.settledPage
            }
        }
    }

    // Mutable list for drag reorder
    val booksList = remember(uiState.allBooks) { uiState.allBooks.toMutableStateList() }

    // Grid drag state (edit mode only)
    val lazyGridState = rememberLazyGridState()
    val gridDragDropState = rememberGridDragDropState(lazyGridState)

    gridDragDropState.onSwap = { fromIdx, toIdx ->
        if (selectedTab == 0) {
            booksList.add(toIdx, booksList.removeAt(fromIdx))
        } else {
            val filteredGlobalIndices = booksList.indices.filter { i ->
                when (selectedTab) {
                    1 -> booksList[i].status == ReadingStatus.WANT_TO_READ
                    2 -> booksList[i].status == ReadingStatus.READING
                    3 -> booksList[i].status == ReadingStatus.COMPLETED
                    else -> true
                }
            }
            val fromGlobal = filteredGlobalIndices.getOrNull(fromIdx)
            val toGlobal = filteredGlobalIndices.getOrNull(toIdx)
            if (fromGlobal != null && toGlobal != null && fromGlobal != toGlobal) {
                booksList.add(toGlobal, booksList.removeAt(fromGlobal))
            }
        }
    }

    fun matchesSearch(book: Book, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return book.title.lowercase().contains(q) ||
                book.author.lowercase().contains(q) ||
                book.publisher.lowercase().contains(q)
    }

    // Filtered + sorted books for normal pager view
    fun booksForPage(page: Int): List<Book> {
        val filtered = when (page) {
            1 -> uiState.allBooks.filter { it.status == ReadingStatus.WANT_TO_READ }
            2 -> uiState.allBooks.filter { it.status == ReadingStatus.READING }
            3 -> uiState.allBooks.filter { it.status == ReadingStatus.COMPLETED }
            else -> uiState.allBooks
        }
        return filtered.filter { matchesSearch(it, searchQuery) }.applySortOrder(sortOrder)
    }

    // Filtered books from mutable list for edit mode grid
    val editModeList: List<Book> = when (selectedTab) {
        1 -> booksList.filter { it.status == ReadingStatus.WANT_TO_READ }
        2 -> booksList.filter { it.status == ReadingStatus.READING }
        3 -> booksList.filter { it.status == ReadingStatus.COMPLETED }
        else -> booksList
    }

    BackHandler(enabled = isSelectionMode) { selectedIds = emptySet() }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("선택 삭제") },
            text = { Text("선택한 ${selectedIds.size}권을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    val count = selectedIds.size
                    uiState.allBooks.filter { it.id in selectedIds }.forEach { viewModel.deleteBook(it) }
                    selectedIds = emptySet()
                    showDeleteConfirm = false
                    scope.launch { snackbarHostState.showSnackbar("${count}권이 삭제됐습니다") }
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            }
        )
    }

    pendingTabChange?.let { targetTab ->
        AlertDialog(
            onDismissRequest = { pendingTabChange = null },
            text = { Text("탭을 이동하면 선택이 해제됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    selectedIds = emptySet()
                    selectedTab = targetTab
                    scope.launch { pagerState.scrollToPage(targetTab) }
                    pendingTabChange = null
                }) { Text("이동") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTabChange = null }) { Text("취소") }
            }
        )
    }

    if (showStatusPicker) {
        AlertDialog(
            onDismissRequest = { showStatusPicker = false },
            title = { Text("독서 상태 변경") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        ReadingStatus.WANT_TO_READ to "독서예정",
                        ReadingStatus.READING to "독서중",
                        ReadingStatus.COMPLETED to "독서완료"
                    ).forEach { (status, label) ->
                        TextButton(
                            onClick = {
                                val count = selectedIds.size
                                uiState.allBooks.filter { it.id in selectedIds }.forEach {
                                    viewModel.updateBook(it.copy(status = status))
                                }
                                selectedIds = emptySet()
                                showStatusPicker = false
                                scope.launch { snackbarHostState.showSnackbar("${count}권의 상태가 변경됐습니다") }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStatusPicker = false }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size}권 선택됨") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    actions = {
                        TextButton(onClick = { selectedIds = emptySet() }) {
                            Text("취소", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                )
            } else if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("제목, 작가, 출판사 검색") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Close, "검색 닫기")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, "입력 지우기", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "서재",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "검색", tint = MaterialTheme.colorScheme.primary)
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, "정렬", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.values().forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (sortOrder == order) {
                                                    Icon(
                                                        Icons.Default.Check, null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                } else {
                                                    Spacer(Modifier.size(14.dp))
                                                }
                                                Text(order.label, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        },
                                        onClick = {
                                            sortOrder = order
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = onAddBook,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, "책 추가")
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStatusPicker = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("상태 변경") }
                        Button(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("삭제") }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            if (isSelectionMode) {
                // Edit mode: grid with drag handles
                if (editModeList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "등록된 책이 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        state = lazyGridState,
                        columns = GridCells.Adaptive(100.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(editModeList, key = { _, book -> book.id }) { index, book ->
                            val isDragging = gridDragDropState.draggingItemIndex == index
                            val isSelected = book.id in selectedIds
                            val currentIndex by rememberUpdatedState(index)

                            Box(
                                modifier = if (isDragging) {
                                    Modifier.zIndex(1f).graphicsLayer {
                                        translationX = gridDragDropState.draggingItemOffset.x
                                        translationY = gridDragDropState.draggingItemOffset.y
                                        scaleX = 1.06f
                                        scaleY = 1.06f
                                        shadowElevation = 16f
                                    }
                                } else Modifier
                            ) {
                                BookGridItem(
                                    book = book,
                                    showStatus = selectedTab == 0,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedIds = if (isSelected) selectedIds - book.id
                                        else selectedIds + book.id
                                    },
                                    onLongClick = {
                                        selectedIds = if (isSelected) selectedIds - book.id
                                        else selectedIds + book.id
                                    }
                                )
                                // Drag handle — visible only in edit mode
                                if (sortOrder == SortOrder.ADDED) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(3.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.38f))
                                            .pointerInput(Unit) {
                                                detectDragGestures(
                                                    onDragStart = { gridDragDropState.startDrag(currentIndex) },
                                                    onDrag = { change, offset ->
                                                        change.consume()
                                                        gridDragDropState.onDrag(offset)
                                                    },
                                                    onDragEnd = {
                                                        viewModel.updateBooksOrder(booksList.toList())
                                                        gridDragDropState.endDrag()
                                                    },
                                                    onDragCancel = { gridDragDropState.endDrag() }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.DragHandle, null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                        item(span = { GridItemSpan(maxCurrentLineSpan) }) { Spacer(Modifier.height(72.dp)) }
                    }
                }
            } else {
                // Normal mode: HorizontalPager with grid per tab
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageBooks = booksForPage(page)
                    if (pageBooks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                if (searchQuery.isNotBlank()) "\"$searchQuery\" 검색 결과가 없습니다"
                                else "등록된 책이 없습니다",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(100.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(pageBooks, key = { it.id }) { book ->
                                BookGridItem(
                                    book = book,
                                    showStatus = page == 0,
                                    isSelected = false,
                                    onClick = { onBookClick(book.id) },
                                    onLongClick = { selectedIds = setOf(book.id) }
                                )
                            }
                            item(span = { GridItemSpan(maxCurrentLineSpan) }) { Spacer(Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }
}
