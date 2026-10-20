package ua.compass.planner.ui.planner

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drag-to-reorder for a LazyColumn. The dragged item follows the finger; when it passes the middle of a
 * neighbour, [onMove] is asked to swap the two keys. The list scrolls by itself near its edges.
 */
class ReorderState(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val edgePx: Float,
    private val maxSpeedPx: Float,
) {
    var onMove: (from: Any, to: Any) -> Boolean = { _, _ -> false }
    var onDragEnd: () -> Unit = {}

    var draggingKey by mutableStateOf<Any?>(null)
        private set

    private var delta by mutableFloatStateOf(0f)
    private var initialOffset = 0
    private var expectedIndex = -1
    private var autoScroll: Job? = null

    private fun current(): LazyListItemInfo? {
        val key = draggingKey ?: return null
        return listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
    }

    /** Translation that keeps the dragged item under the finger. */
    val offset: Float
        get() = current()?.let { initialOffset + delta - it.offset } ?: 0f

    fun start(key: Any) {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        draggingKey = key
        initialOffset = info.offset
        delta = 0f
        expectedIndex = info.index
        autoScroll?.cancel()
        autoScroll = scope.launch {
            while (isActive && draggingKey != null) {
                scrollStep()
                withFrameNanos { }
            }
        }
    }

    fun drag(dy: Float) {
        if (draggingKey == null) return
        delta += dy
        trySwap()
    }

    fun end() {
        if (draggingKey == null) return
        autoScroll?.cancel()
        draggingKey = null
        delta = 0f
        onDragEnd()
    }

    private fun trySwap() {
        val item = current() ?: return
        // Wait until the previous swap has been laid out.
        if (item.index != expectedIndex) return
        val visible = listState.layoutInfo.visibleItemsInfo
        val top = initialOffset + delta
        val bottom = top + item.size
        val next = visible.firstOrNull { it.index == item.index + 1 }
        val prev = visible.firstOrNull { it.index == item.index - 1 }
        val target = when {
            next != null && bottom > next.offset + next.size / 2f -> next
            prev != null && top < prev.offset + prev.size / 2f -> prev
            else -> null
        } ?: return
        val firstIndex = listState.firstVisibleItemIndex
        val firstOffset = listState.firstVisibleItemScrollOffset
        if (onMove(item.key, target.key)) {
            expectedIndex = target.index
            // Lazy lists keep the first visible item in place; undo that when it is part of the swap.
            if (firstIndex == item.index || firstIndex == target.index) {
                scope.launch { listState.scrollToItem(firstIndex, firstOffset) }
            }
        }
    }

    private suspend fun scrollStep() {
        val item = current() ?: return
        val info = listState.layoutInfo
        val top = initialOffset + delta
        val bottom = top + item.size
        val speed = when {
            delta > 0 && bottom > info.viewportEndOffset - edgePx ->
                ((bottom - (info.viewportEndOffset - edgePx)) / edgePx).coerceIn(0f, 1f) * maxSpeedPx
            delta < 0 && top < info.viewportStartOffset + edgePx ->
                -(((info.viewportStartOffset + edgePx) - top) / edgePx).coerceIn(0f, 1f) * maxSpeedPx
            else -> 0f
        }
        if (speed != 0f) {
            listState.scrollBy(speed)
            trySwap()
        }
    }
}

/**
 * Starts a drag on long press anywhere on the item, or immediately when the touch begins on the
 * drag handle at the item's end edge.
 */
fun Modifier.reorderable(key: Any, state: ReorderState, handleWidthPx: Float, onStart: () -> Unit): Modifier =
    pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val fromHandle = down.position.x >= size.width - handleWidthPx
            val startChange = if (fromHandle) {
                awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
            } else {
                awaitLongPressOrCancellation(down.id)
            }
            if (startChange != null) {
                onStart()
                state.start(key)
                try {
                    drag(startChange.id) { change ->
                        state.drag(change.positionChange().y)
                        change.consume()
                    }
                } finally {
                    state.end()
                }
            }
        }
    }
