@file:OptIn(ExperimentalMaterial3Api::class)

package ua.compass.planner.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.compass.planner.R
import ua.compass.planner.data.Quadrant
import ua.compass.planner.data.Task
import ua.compass.planner.data.quadrant
import ua.compass.planner.ui.AppViewModel
import ua.compass.planner.ui.components.DrawerButton
import ua.compass.planner.ui.components.taskColor
import ua.compass.planner.ui.theme.quadrantColor
import ua.compass.planner.util.Dates
import ua.compass.planner.util.currentLocale
import java.time.LocalDate
import kotlin.math.roundToInt

private class MatrixDrag(val task: Task, val origin: Offset, val size: IntSize) {
    var delta by mutableStateOf(Offset.Zero)
    val center: Offset get() = origin + delta + Offset(size.width / 2f, size.height / 2f)
}

private class CoordsHolder {
    var coords: LayoutCoordinates? = null
}

private val Quadrant.title: Int
    get() = when (this) {
        Quadrant.I -> R.string.q1_title
        Quadrant.II -> R.string.q2_title
        Quadrant.III -> R.string.q3_title
        Quadrant.IV -> R.string.q4_title
    }

private val Quadrant.action: Int
    get() = when (this) {
        Quadrant.I -> R.string.q1_action
        Quadrant.II -> R.string.q2_action
        Quadrant.III -> R.string.q3_action
        Quadrant.IV -> R.string.q4_action
    }

@Composable
fun MatrixScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val roles = remember(data.roles) { data.roles.associateBy { it.id } }
    val goals = remember(data.goals) { data.goals.associateBy { it.id } }
    val byQuadrant = remember(data.tasks) {
        data.tasks.filter { !it.done }
            .sortedWith(compareBy(nullsLast<Long>()) { it.date })
            .groupBy { it.quadrant }
    }
    val bounds = remember { mutableStateMapOf<Quadrant, Rect>() }
    var drag by remember { mutableStateOf<MatrixDrag?>(null) }
    var boxOrigin by remember { mutableStateOf(Offset.Zero) }
    val hovered = drag?.let { d -> bounds.entries.firstOrNull { it.value.contains(d.center) }?.key }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.matrix_title)) }, navigationIcon = { DrawerButton() }) },
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp)
                .onGloballyPositioned { boxOrigin = it.positionInRoot() }
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(listOf(Quadrant.I, Quadrant.II), listOf(Quadrant.III, Quadrant.IV)).forEach { row ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { q ->
                            QuadrantCard(
                                quadrant = q,
                                tasks = byQuadrant[q].orEmpty(),
                                highlighted = hovered == q && drag?.task?.quadrant != q,
                                draggingId = drag?.task?.id,
                                colorOf = { taskColor(it, roles, goals) },
                                onAdd = { vm.newTask(Dates.todayEpoch(), quadrant = q) },
                                onOpen = { vm.editTask(it) },
                                onDragStart = { drag = it },
                                onDrag = { amount -> drag?.let { it.delta += amount } },
                                onDrop = {
                                    drag?.let { d ->
                                        val target = bounds.entries.firstOrNull { it.value.contains(d.center) }?.key
                                        if (target != null && target != d.task.quadrant) vm.setQuadrant(d.task.id, target)
                                    }
                                    drag = null
                                },
                                onCancel = { drag = null },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .onGloballyPositioned { bounds[q] = it.boundsInRoot() },
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.matrix_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            drag?.let { d ->
                val density = LocalDensity.current
                MatrixChip(
                    task = d.task,
                    color = taskColor(d.task, roles, goals),
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (d.origin.x + d.delta.x - boxOrigin.x).roundToInt(),
                                (d.origin.y + d.delta.y - boxOrigin.y).roundToInt(),
                            )
                        }
                        .width(with(density) { d.size.width.toDp() })
                        .shadow(10.dp, RoundedCornerShape(10.dp)),
                )
            }
        }
    }
}

@Composable
private fun QuadrantCard(
    quadrant: Quadrant,
    tasks: List<Task>,
    highlighted: Boolean,
    draggingId: Long?,
    colorOf: (Task) -> Color,
    onAdd: () -> Unit,
    onOpen: (Task) -> Unit,
    onDragStart: (MatrixDrag) -> Unit,
    onDrag: (Offset) -> Unit,
    onDrop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = quadrantColor(quadrant)
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) accent.copy(alpha = 0.15f) else scheme.surfaceContainerLow
        ),
        border = when {
            highlighted -> BorderStroke(2.dp, accent)
            quadrant == Quadrant.II -> BorderStroke(1.dp, accent.copy(alpha = 0.6f))
            else -> null
        },
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(quadrant.name, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(quadrant.action),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.add_task), Modifier.size(20.dp))
                }
            }
            Text(
                stringResource(quadrant.title),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                tasks.forEach { task ->
                    val holder = remember(task.id) { CoordsHolder() }
                    MatrixChip(
                        task = task,
                        color = colorOf(task),
                        onClick = { onOpen(task) },
                        modifier = Modifier
                            .onGloballyPositioned { holder.coords = it }
                            .alpha(if (draggingId == task.id) 0f else 1f)
                            .pointerInput(task.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        holder.coords?.let { c ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onDragStart(MatrixDrag(task, c.positionInRoot(), c.size))
                                        }
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        onDrag(amount)
                                    },
                                    onDragEnd = onDrop,
                                    onDragCancel = onCancel,
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixChip(task: Task, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val locale = currentLocale()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(task.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val date = task.date
                Text(
                    if (date == null) stringResource(R.string.inbox)
                    else Dates.format(LocalDate.ofEpochDay(date), "EE, d MMM", locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
