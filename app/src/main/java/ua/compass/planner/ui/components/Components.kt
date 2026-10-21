@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ua.compass.planner.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.compass.planner.R
import ua.compass.planner.data.Goal
import ua.compass.planner.data.Horizon
import ua.compass.planner.data.horizon
import ua.compass.planner.data.Palette
import ua.compass.planner.data.Quadrant
import ua.compass.planner.data.Role
import ua.compass.planner.data.Task
import ua.compass.planner.data.quadrant
import ua.compass.planner.reminders.Notifications
import ua.compass.planner.ui.theme.quadrantColor
import ua.compass.planner.util.Dates
import ua.compass.planner.util.LocalFirstDayOfWeek
import ua.compass.planner.util.currentLocale
import java.time.LocalDate

fun taskColor(task: Task, roles: Map<Long, Role>, goals: Map<Long, Goal>): Color = Color(
    task.color ?: task.goalId?.let { goals[it]?.color } ?: task.roleId?.let { roles[it]?.color } ?: Palette.DEFAULT
)

fun taskMeta(task: Task, roles: Map<Long, Role>, goals: Map<Long, Goal>): String = listOfNotNull(
    task.time?.let(Dates::time),
    task.goalId?.let { goals[it]?.title } ?: task.roleId?.let { roles[it]?.name },
).joinToString(" • ")

@Composable
fun TaskCard(
    task: Task,
    color: Color,
    meta: String,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragging: Boolean = false,
    showHandle: Boolean = true,
) {
    val elevation by animateDpAsState(if (dragging) 10.dp else 0.dp, label = "elevation")
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = scheme.surfaceContainerLow,
        shadowElevation = elevation,
        border = if (dragging) BorderStroke(1.5.dp, color) else null,
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = color, uncheckedColor = color),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.done) scheme.onSurfaceVariant else scheme.onSurface,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (task.reminderAt != null) {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp),
                    tint = scheme.onSurfaceVariant,
                )
            }
            QuadrantBadge(task.quadrant, Modifier.padding(start = 6.dp))
            if (showHandle) {
                Icon(
                    Icons.Rounded.DragIndicator,
                    contentDescription = stringResource(R.string.drag),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(24.dp),
                    tint = scheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

@Composable
fun QuadrantBadge(q: Quadrant, modifier: Modifier = Modifier) {
    val color = quadrantColor(q)
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(q.name, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ColorDot(color: Color, size: Int = 12) {
    Box(
        Modifier
            .size(size.dp)
            .background(color, CircleShape)
    )
}

@Composable
fun goalPeriodLabel(goal: Goal): String {
    val locale = currentLocale()
    val firstDay = LocalFirstDayOfWeek.current
    return when (goal.horizon) {
        Horizon.WEEK -> Dates.weekRange(Dates.weekStart(LocalDate.ofEpochDay(goal.weekStart!!), firstDay), locale)
        Horizon.MONTH -> Dates.format(Dates.yearMonth(goal.month!!).atDay(1), "LLLL yyyy", locale)
        Horizon.LONG -> stringResource(R.string.goal_long_term)
    }
}

@Composable
fun GoalLine(
    goal: Goal,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    subtitle: String = goalPeriodLabel(goal),
    trailing: @Composable (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = goal.done,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Color(goal.color), uncheckedColor = Color(goal.color)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                goal.title,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (goal.done) TextDecoration.LineThrough else null,
                color = if (goal.done) scheme.onSurfaceVariant else scheme.onSurface,
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

/** "Goals of the week/month" card shown above the calendar. */
@Composable
fun PeriodGoalsCard(
    title: String,
    goals: List<Goal>,
    tasks: List<Task>,
    onAddGoal: () -> Unit,
    onToggle: (Goal) -> Unit,
    onEdit: (Goal) -> Unit,
    onAddTask: (Goal) -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    SectionCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Flag, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (goals.isNotEmpty()) {
                Text(
                    "${goals.count { it.done }}/${goals.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onAddGoal, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Add, stringResource(R.string.add_goal))
            }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
        }
        if (!expanded) return@SectionCard
        if (goals.isEmpty()) {
            Text(
                stringResource(R.string.period_goals_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        goals.sortedBy { it.done }.forEach { goal ->
            GoalLine(
                goal = goal,
                onToggle = { onToggle(goal) },
                onClick = { onEdit(goal) },
                subtitle = goalTasksLabel(goal, tasks),
                trailing = {
                    IconButton(onClick = { onAddTask(goal) }) {
                        Icon(Icons.Rounded.AddTask, stringResource(R.string.add_task_for_goal), Modifier.size(20.dp))
                    }
                },
            )
        }
    }
}

@Composable
fun goalTasksLabel(goal: Goal, tasks: List<Task>): String {
    val own = tasks.filter { it.goalId == goal.id }
    return if (own.isEmpty()) stringResource(R.string.goal_no_tasks)
    else stringResource(R.string.goal_tasks_progress, own.count { it.done }, own.size)
}

@Composable
fun PeriodNavigator(label: String, onPrev: () -> Unit, onNext: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous)) }
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next)) }
    }
}

/** Opens the side menu; provided by the main screen. */
val LocalOpenDrawer = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun DrawerButton() {
    val open = LocalOpenDrawer.current
    IconButton(onClick = open) { Icon(Icons.Rounded.Menu, stringResource(R.string.open_menu)) }
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun EmptyState(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        Spacer(Modifier.height(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
fun SubScaffold(
    title: String,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { DrawerButton() },
            )
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Composable
fun EditorScaffold(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.cancel)) }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete)) }
                    }
                    TextButton(onClick = onSave, enabled = canSave) { Text(stringResource(R.string.save)) }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .consumeWindowInsets(pad)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
fun ConfirmDialog(text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ColorPicker(selected: Long?, onSelect: (Long?) -> Unit, allowAuto: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (allowAuto) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (selected == null) scheme.primary else scheme.outlineVariant, CircleShape)
                    .clickable { onSelect(null) },
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.color_auto_short), style = MaterialTheme.typography.labelLarge)
            }
        }
        Palette.colors.forEach { c ->
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .clickable { onSelect(c) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected == c) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun <T> Selector(
    label: String,
    value: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    colorOf: (T) -> Color? = { null },
) {
    var open by remember { mutableStateOf(false) }
    val text = options.firstOrNull { it.first == value }?.second ?: options.firstOrNull()?.second.orEmpty()
    Box {
        OutlinedCard(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                val c = colorOf(value)
                if (c != null) {
                    ColorDot(c)
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text, style = MaterialTheme.typography.bodyLarge)
                }
                Icon(Icons.Rounded.ArrowDropDown, null)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (v, name) ->
                val c = colorOf(v)
                DropdownMenuItem(
                    text = { Text(name) },
                    leadingIcon = if (c != null) {
                        { ColorDot(c) }
                    } else null,
                    onClick = {
                        onSelect(v)
                        open = false
                    },
                )
            }
        }
    }
}

private const val DAY_MS = 86_400_000L

@Composable
fun DatePickDialog(initial: LocalDate?, onDismiss: () -> Unit, onPick: (LocalDate?) -> Unit, allowNone: Boolean = false) {
    val state = rememberDatePickerState(initialSelectedDateMillis = (initial ?: LocalDate.now()).toEpochDay() * DAY_MS)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(LocalDate.ofEpochDay(Math.floorDiv(it, DAY_MS))) }
                onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            Row {
                if (allowNone) {
                    TextButton(onClick = { onPick(null); onDismiss() }) { Text(stringResource(R.string.no_date)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun TimePickDialog(initialMinutes: Int?, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val start = initialMinutes ?: (9 * 60)
    val state = rememberTimePickerState(initialHour = start / 60, initialMinute = start % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(state.hour * 60 + state.minute); onDismiss() }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = { TimePicker(state = state) },
    )
}

/** Asks for the notification permission on Android 13+ the first time a reminder is switched on. */
@Composable
fun rememberNotificationPermission(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return {
        if (Build.VERSION.SDK_INT >= 33 && !Notifications.canNotify(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun ReminderField(value: Long?, defaultMillis: () -> Long, onChange: (Long?) -> Unit) {
    val locale = currentLocale()
    val askPermission = rememberNotificationPermission()
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.reminder_enable), Modifier.weight(1f))
        Switch(checked = value != null, onCheckedChange = { on ->
            if (on) {
                askPermission()
                onChange(defaultMillis())
            } else onChange(null)
        })
    }
    if (value != null) {
        val dt = Dates.dateTime(value)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { pickDate = true },
                label = { Text(Dates.format(dt.toLocalDate(), "EEE, d MMM yyyy", locale)) },
                leadingIcon = { Icon(Icons.Rounded.Event, null, Modifier.size(18.dp)) },
            )
            AssistChip(
                onClick = { pickTime = true },
                label = { Text(Dates.time(dt.hour * 60 + dt.minute)) },
                leadingIcon = { Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp)) },
            )
        }
        if (value < System.currentTimeMillis()) {
            Text(stringResource(R.string.reminder_past), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (pickDate) {
            DatePickDialog(dt.toLocalDate(), { pickDate = false }, { d ->
                if (d != null) onChange(Dates.millis(d, dt.hour * 60 + dt.minute))
            })
        }
        if (pickTime) {
            TimePickDialog(dt.hour * 60 + dt.minute, { pickTime = false }, { m ->
                onChange(Dates.millis(dt.toLocalDate(), m))
            })
        }
    }
}
