package com.g1.booklog.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset

// ── LazyColumn drag-drop (kept for reference) ────────────────────────────

class DragDropState(val lazyListState: LazyListState) {
    var onSwap: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }

    var draggingIndex by mutableStateOf<Int?>(null)
        private set

    private var totalDelta = 0f
    private var initialItemOffset = 0

    val draggingItemTranslationY: Float
        get() {
            val idx = draggingIndex ?: return 0f
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == idx } ?: return 0f
            return initialItemOffset + totalDelta - itemInfo.offset
        }

    fun startDrag(fromIndex: Int) {
        draggingIndex = fromIndex
        totalDelta = 0f
        initialItemOffset = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == fromIndex }?.offset ?: 0
    }

    fun onDrag(deltaY: Float) {
        totalDelta += deltaY
        val fromIdx = draggingIndex ?: return
        val visItems = lazyListState.layoutInfo.visibleItemsInfo
        val fromItem = visItems.firstOrNull { it.index == fromIdx } ?: return
        val center = fromItem.offset + fromItem.size / 2 + draggingItemTranslationY.toInt()
        val target = visItems.firstOrNull { item ->
            item.index != fromIdx && center in item.offset..(item.offset + item.size)
        } ?: return
        onSwap(fromIdx, target.index)
        initialItemOffset += fromItem.offset - target.offset
        draggingIndex = target.index
    }

    fun endDrag() {
        draggingIndex = null
        totalDelta = 0f
        initialItemOffset = 0
    }
}

@Composable
fun rememberDragDropState(lazyListState: LazyListState): DragDropState {
    return remember(lazyListState) { DragDropState(lazyListState) }
}

// ── LazyVerticalGrid drag-drop ───────────────────────────────────────────

class GridDragDropState(val lazyGridState: LazyGridState) {
    var onSwap: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> }

    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    private var totalDelta = Offset.Zero
    private var initialItemCenter = Offset.Zero

    val draggingItemOffset: Offset
        get() {
            val idx = draggingItemIndex ?: return Offset.Zero
            val item = lazyGridState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == idx } ?: return Offset.Zero
            val naturalCenter = Offset(
                item.offset.x + item.size.width / 2f,
                item.offset.y + item.size.height / 2f
            )
            return initialItemCenter + totalDelta - naturalCenter
        }

    fun startDrag(fromIndex: Int) {
        draggingItemIndex = fromIndex
        totalDelta = Offset.Zero
        val item = lazyGridState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == fromIndex }
        initialItemCenter = if (item != null) {
            Offset(item.offset.x + item.size.width / 2f, item.offset.y + item.size.height / 2f)
        } else Offset.Zero
    }

    fun onDrag(delta: Offset) {
        totalDelta += delta
        val fromIdx = draggingItemIndex ?: return
        val dragCenter = initialItemCenter + totalDelta
        // Swap when the drag center crosses 33% into a neighboring item
        val target = lazyGridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            if (item.index == fromIdx) return@firstOrNull false
            val thresholdX = item.size.width * 0.33f
            val thresholdY = item.size.height * 0.33f
            dragCenter.x in (item.offset.x + thresholdX)..(item.offset.x + item.size.width - thresholdX) &&
            dragCenter.y in (item.offset.y + thresholdY)..(item.offset.y + item.size.height - thresholdY)
        } ?: return
        onSwap(fromIdx, target.index)
        initialItemCenter = dragCenter
        totalDelta = Offset.Zero
        draggingItemIndex = target.index
    }

    fun endDrag() {
        draggingItemIndex = null
        totalDelta = Offset.Zero
        initialItemCenter = Offset.Zero
    }
}

@Composable
fun rememberGridDragDropState(lazyGridState: LazyGridState): GridDragDropState {
    return remember(lazyGridState) { GridDragDropState(lazyGridState) }
}
