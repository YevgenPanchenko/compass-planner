package ua.compass.planner.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ua.compass.planner.R
import ua.compass.planner.data.AppData
import ua.compass.planner.data.Task
import ua.compass.planner.ui.AppViewModel
import ua.compass.planner.ui.Placement
import ua.compass.planner.ui.components.TaskCard
import ua.compass.planner.ui.components.taskColor
import ua.compass.planner.ui.components.taskMeta

/** A day (or the inbox when [date] is null) in the planner list. */
@Immutable
data class Section(val key: String, val date: Long?, val title: String, val isToday: Boolean)

private sealed interface Entry {
    val key: String
}

private data class HeaderEntry(val section: Section) : Entry {
    override val key: String get() = "h_" + section.key
}

private data class TaskEntry(val task: Task) : Entry {
    override val key: String get() = "t_" + task.id
}

private fun buildEntries(tasks: List<Task>, sections: List<Section>): List<Entry> = buildList {
    sections.forEach { section ->
        add(HeaderEntry(section))
        tasks.filter { it.date == section.date && (section.date != null || !it.done) }
            .sortedWith(compareBy({ it.order }, { it.id }))
            .forEach { add(TaskEntry(it)) }
    }
}

private fun placements(entries: List<Entry>): List<Placement> {
    val out = mutableListOf<Placement>()
    var date: Long? = null
    var order = 0
    entries.forEach { e ->
        when (e) {
            is HeaderEntry -> {
                date = e.section.date
                order = 0
            }
            is TaskEntry -> out += Placement(e.task.id, date, order++)
        }
    }
    return out
}

/**
 * Tasks grouped under day headers. Tasks can be dragged within a day and across days; the new
 * dates and order are saved when the finger is lifted.
 */
@Composable
fun PlannerList(
    vm: AppViewModel,
    data: AppData,
    sections: List<Section>,
    onHeaderClick: (Section) -> Unit,
    prefix: LazyListScope.() -> Unit = {},
) {
    val source = remember(data.tasks, sections) { buildEntries(data.tasks, sections) }
    var local by remember { mutableStateOf<List<Entry>?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val reorder = remember(listState) {
        with(density) { ReorderState(listState, scope, edgePx = 72.dp.toPx(), maxSpeedPx = 18.dp.toPx()) }
    }
    val handleWidth = with(density) { 48.dp.toPx() }

    // Drop the local copy once the saved order has come back from the store.
    LaunchedEffect(source) {
        if (reorder.draggingKey == null) local = null
    }

    reorder.onMove = { from, to ->
        val current = local ?: source
        val fromIndex = current.indexOfFirst { it.key == from }
        val toIndex = current.indexOfFirst { it.key == to }
        // Index 0 is the first header: nothing may go above it.
        if (fromIndex < 0 || toIndex <= 0) false
        else {
            local = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            true
        }
    }
    reorder.onDragEnd = { local?.let { vm.applyPlacements(placements(it)) } }

    val entries = local ?: source
    val roles = remember(data.roles) { data.roles.associateBy { it.id } }
    val goals = remember(data.goals) { data.goals.associateBy { it.id } }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        prefix()
        entries.forEachIndexed { index, entry ->
            when (entry) {
                is HeaderEntry -> item(key = entry.key, contentType = "header") {
                    SectionHeader(
                        section = entry.section,
                        empty = entries.getOrNull(index + 1) !is TaskEntry,
                        onAdd = { vm.newTask(entry.section.date) },
                        onClick = { onHeaderClick(entry.section) },
                        modifier = Modifier.animateItem(),
                    )
                }
                is TaskEntry -> item(key = entry.key, contentType = "task") {
                    val dragging = reorder.draggingKey == entry.key
                    val task = entry.task
                    TaskCard(
                        task = task,
                        color = taskColor(task, roles, goals),
                        meta = taskMeta(task, roles, goals),
                        onToggle = { vm.toggleTask(task.id) },
                        onClick = { vm.editTask(task) },
                        dragging = dragging,
                        modifier = (if (dragging) {
                            Modifier
                                .zIndex(1f)
                                .graphicsLayer { translationY = reorder.offset }
                        } else Modifier.animateItem())
                            .reorderable(entry.key, reorder, handleWidth) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(section: Section, empty: Boolean, onAdd: () -> Unit, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (section.date == null) {
                    Icon(Icons.Rounded.Inbox, null, Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    section.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (section.isToday) scheme.primary else scheme.onSurface,
                )
                if (section.isToday) {
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(50), color = scheme.primary) {
                        Text(
                            stringResource(R.string.today),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Add, stringResource(R.string.add_task), tint = scheme.primary)
            }
        }
        if (empty) {
            Text(
                stringResource(if (section.date == null) R.string.inbox_empty else R.string.day_empty),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}
