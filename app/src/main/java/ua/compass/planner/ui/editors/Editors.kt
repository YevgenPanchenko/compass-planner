@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ua.compass.planner.ui.editors

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.compass.planner.R
import ua.compass.planner.data.AppData
import ua.compass.planner.data.Goal
import ua.compass.planner.data.Habit
import ua.compass.planner.data.Horizon
import ua.compass.planner.data.horizon
import ua.compass.planner.data.JournalEntry
import ua.compass.planner.data.Quadrant
import ua.compass.planner.data.Role
import ua.compass.planner.data.Task
import ua.compass.planner.data.quadrant
import ua.compass.planner.ui.AppViewModel
import ua.compass.planner.ui.EditorRequest
import ua.compass.planner.ui.components.*
import ua.compass.planner.util.Dates
import ua.compass.planner.util.LocalFirstDayOfWeek
import ua.compass.planner.util.currentLocale
import java.time.LocalDate
import java.time.YearMonth

/** Full-screen editor shown above the tabs while the view model holds an [EditorRequest]. */
@Composable
fun EditorHost(vm: AppViewModel) {
    val request by vm.editor.collectAsStateWithLifecycle()
    val data by vm.data.collectAsStateWithLifecycle()
    val r = request ?: return
    BackHandler { vm.closeEditor() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        key(r) {
            when (r) {
                is EditorRequest.TaskEdit -> TaskEditor(
                    r.task, data,
                    onSave = vm::saveTask,
                    onDelete = { vm.deleteTask(r.task.id) },
                    onClose = vm::closeEditor,
                )
                is EditorRequest.GoalEdit -> GoalEditor(
                    r.goal, r.refDay, data,
                    onSave = vm::saveGoal,
                    onDelete = { vm.deleteGoal(r.goal.id) },
                    onClose = vm::closeEditor,
                )
                is EditorRequest.RoleEdit -> RoleEditor(
                    r.role,
                    onSave = vm::saveRole,
                    onDelete = { vm.deleteRole(r.role.id) },
                    onClose = vm::closeEditor,
                )
                is EditorRequest.HabitEdit -> HabitEditor(
                    r.habit,
                    onSave = vm::saveHabit,
                    onDelete = { vm.deleteHabit(r.habit.id) },
                    onClose = vm::closeEditor,
                )
                is EditorRequest.JournalEdit -> JournalEditor(
                    r.entry,
                    onSave = vm::saveJournal,
                    onDelete = { vm.deleteJournal(r.entry.id) },
                    onClose = vm::closeEditor,
                )
            }
        }
    }
}

private val Quadrant.hint: Int
    get() = when (this) {
        Quadrant.I -> R.string.q1_hint
        Quadrant.II -> R.string.q2_hint
        Quadrant.III -> R.string.q3_hint
        Quadrant.IV -> R.string.q4_hint
    }

@Composable
private fun TaskEditor(initial: Task, data: AppData, onSave: (Task) -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    var t by remember { mutableStateOf(initial) }
    val isNew = initial.id == 0L
    val locale = currentLocale()
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val roles = data.roles.sortedBy { it.order }
    val noneLabel = stringResource(R.string.none)

    EditorScaffold(
        title = stringResource(if (isNew) R.string.task_new else R.string.task_edit),
        canSave = t.title.isNotBlank(),
        onSave = { onSave(t.copy(title = t.title.trim())) },
        onClose = onClose,
        onDelete = if (isNew) null else ({ confirmDelete = true }),
    ) {
        OutlinedTextField(
            value = t.title,
            onValueChange = { t = t.copy(title = it) },
            label = { Text(stringResource(R.string.field_title)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        OutlinedTextField(
            value = t.notes,
            onValueChange = { t = t.copy(notes = it) },
            label = { Text(stringResource(R.string.field_notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        FieldLabel(stringResource(R.string.label_when))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val date = t.date
            AssistChip(
                onClick = { pickDate = true },
                label = {
                    Text(
                        if (date == null) stringResource(R.string.no_date_inbox)
                        else Dates.format(LocalDate.ofEpochDay(date), "EEE, d MMM yyyy", locale)
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Event, null, Modifier.size(18.dp)) },
            )
            if (date != null) {
                val time = t.time
                AssistChip(
                    onClick = { pickTime = true },
                    label = { Text(if (time == null) stringResource(R.string.add_time) else Dates.time(time)) },
                    leadingIcon = { Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp)) },
                    trailingIcon = if (time != null) {
                        {
                            IconButton(onClick = { t = t.copy(time = null) }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Rounded.Close, stringResource(R.string.clear), Modifier.size(16.dp))
                            }
                        }
                    } else null,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val today = Dates.todayEpoch()
            SuggestionChip(onClick = { t = t.copy(date = today) }, label = { Text(stringResource(R.string.today)) })
            SuggestionChip(onClick = { t = t.copy(date = today + 1) }, label = { Text(stringResource(R.string.tomorrow)) })
            SuggestionChip(onClick = { t = t.copy(date = null) }, label = { Text(stringResource(R.string.inbox)) })
        }

        FieldLabel(stringResource(R.string.label_role_goal))
        val goalOptions = data.goals.filter { !it.done || it.id == t.goalId }
            .sortedWith(compareBy({ it.horizon }, { -(it.weekStart ?: 0L) }, { -(it.month ?: 0) }))
        Selector(
            label = stringResource(R.string.field_goal),
            value = t.goalId,
            options = listOf<Pair<Long?, String>>(null to noneLabel) + goalOptions.map { it.id to it.title },
            colorOf = { id -> data.goals.firstOrNull { it.id == id }?.let { Color(it.color) } },
            onSelect = { id ->
                val goal = data.goals.firstOrNull { it.id == id }
                t = t.copy(goalId = id, roleId = goal?.roleId ?: t.roleId)
            },
        )
        Selector(
            label = stringResource(R.string.field_role),
            value = t.roleId,
            options = listOf<Pair<Long?, String>>(null to noneLabel) + roles.map { it.id to it.name },
            colorOf = { id -> roles.firstOrNull { it.id == id }?.let { Color(it.color) } },
            onSelect = { id -> t = t.copy(roleId = id) },
        )

        FieldLabel(stringResource(R.string.label_priority))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = t.important,
                onClick = { t = t.copy(important = !t.important) },
                label = { Text(stringResource(R.string.important)) },
                leadingIcon = if (t.important) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                } else null,
            )
            FilterChip(
                selected = t.urgent,
                onClick = { t = t.copy(urgent = !t.urgent) },
                label = { Text(stringResource(R.string.urgent)) },
                leadingIcon = if (t.urgent) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) }
                } else null,
            )
            QuadrantBadge(t.quadrant)
        }
        Text(
            stringResource(t.quadrant.hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FieldLabel(stringResource(R.string.label_color))
        ColorPicker(selected = t.color, onSelect = { t = t.copy(color = it) }, allowAuto = true)
        Text(
            stringResource(R.string.color_auto_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FieldLabel(stringResource(R.string.label_reminder))
        ReminderField(
            value = t.reminderAt,
            defaultMillis = {
                Dates.millis(t.date?.let(LocalDate::ofEpochDay) ?: LocalDate.now(), (t.time ?: (9 * 60 + 15)) - 15)
            },
            onChange = { t = t.copy(reminderAt = it) },
        )
        Spacer(Modifier.height(24.dp))
    }

    if (pickDate) {
        DatePickDialog(
            initial = t.date?.let(LocalDate::ofEpochDay),
            onDismiss = { pickDate = false },
            onPick = { d -> t = t.copy(date = d?.toEpochDay()) },
            allowNone = true,
        )
    }
    if (pickTime) {
        TimePickDialog(t.time, { pickTime = false }, { m -> t = t.copy(time = m) })
    }
    if (confirmDelete) {
        ConfirmDialog(stringResource(R.string.delete_task_confirm), stringResource(R.string.delete), onDelete) { confirmDelete = false }
    }
}

@Composable
private fun GoalEditor(
    initial: Goal,
    refDay: Long,
    data: AppData,
    onSave: (Goal) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var g by remember { mutableStateOf(initial) }
    val isNew = initial.id == 0L
    val locale = currentLocale()
    var confirmDelete by remember { mutableStateOf(false) }
    val roles = data.roles.sortedBy { it.order }
    val firstDay = LocalFirstDayOfWeek.current
    val ref = LocalDate.ofEpochDay(refDay)
    val defaultWeek = Dates.weekStart(ref, firstDay).toEpochDay()
    val defaultMonth = Dates.monthIndex(YearMonth.from(ref))

    EditorScaffold(
        title = stringResource(if (isNew) R.string.goal_new else R.string.goal_edit),
        canSave = g.title.isNotBlank(),
        onSave = { onSave(g.copy(title = g.title.trim())) },
        onClose = onClose,
        onDelete = if (isNew) null else ({ confirmDelete = true }),
    ) {
        OutlinedTextField(
            value = g.title,
            onValueChange = { g = g.copy(title = it) },
            label = { Text(stringResource(R.string.field_goal_title)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        OutlinedTextField(
            value = g.notes,
            onValueChange = { g = g.copy(notes = it) },
            label = { Text(stringResource(R.string.field_goal_why)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        Selector(
            label = stringResource(R.string.field_role),
            value = g.roleId,
            options = listOf<Pair<Long?, String>>(null to stringResource(R.string.none)) + roles.map { it.id to it.name },
            colorOf = { id -> roles.firstOrNull { it.id == id }?.let { Color(it.color) } },
            onSelect = { id ->
                val role = roles.firstOrNull { it.id == id }
                g = g.copy(roleId = id, color = role?.color ?: g.color)
            },
        )

        FieldLabel(stringResource(R.string.label_horizon))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Horizon.entries.forEachIndexed { i, h ->
                SegmentedButton(
                    selected = g.horizon == h,
                    onClick = {
                        g = when (h) {
                            Horizon.WEEK -> g.copy(weekStart = g.weekStart ?: defaultWeek, month = null)
                            Horizon.MONTH -> g.copy(weekStart = null, month = g.month ?: defaultMonth)
                            Horizon.LONG -> g.copy(weekStart = null, month = null)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(i, Horizon.entries.size),
                ) {
                    Text(
                        stringResource(
                            when (h) {
                                Horizon.WEEK -> R.string.horizon_week
                                Horizon.MONTH -> R.string.horizon_month
                                Horizon.LONG -> R.string.horizon_long
                            }
                        ),
                        maxLines = 1,
                    )
                }
            }
        }
        val ws = g.weekStart?.let { Dates.weekStart(LocalDate.ofEpochDay(it), firstDay) }
        val mi = g.month
        when {
            ws != null -> PeriodNavigator(
                Dates.weekRange(ws, locale),
                onPrev = { g = g.copy(weekStart = ws.minusWeeks(1).toEpochDay()) },
                onNext = { g = g.copy(weekStart = ws.plusWeeks(1).toEpochDay()) },
            )
            mi != null -> PeriodNavigator(
                Dates.format(Dates.yearMonth(mi).atDay(1), "LLLL yyyy", locale),
                onPrev = { g = g.copy(month = mi - 1) },
                onNext = { g = g.copy(month = mi + 1) },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = g.done, onCheckedChange = { g = g.copy(done = it) })
            Text(stringResource(R.string.goal_done))
        }

        FieldLabel(stringResource(R.string.label_color))
        ColorPicker(selected = g.color, onSelect = { c -> if (c != null) g = g.copy(color = c) })

        FieldLabel(stringResource(R.string.label_reminder))
        ReminderField(
            value = g.reminderAt,
            defaultMillis = {
                val start = g.weekStart?.let(LocalDate::ofEpochDay)
                    ?: g.month?.let { Dates.yearMonth(it).atDay(1) }
                    ?: LocalDate.now()
                Dates.millis(if (start.isBefore(LocalDate.now())) LocalDate.now() else start, 9 * 60)
            },
            onChange = { g = g.copy(reminderAt = it) },
        )
        Spacer(Modifier.height(24.dp))
    }
    if (confirmDelete) {
        ConfirmDialog(stringResource(R.string.delete_goal_confirm), stringResource(R.string.delete), onDelete) { confirmDelete = false }
    }
}

@Composable
private fun RoleEditor(initial: Role, onSave: (Role) -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    var r by remember { mutableStateOf(initial) }
    val isNew = initial.id == 0L
    var confirmDelete by remember { mutableStateOf(false) }
    EditorScaffold(
        title = stringResource(if (isNew) R.string.role_new else R.string.role_edit),
        canSave = r.name.isNotBlank(),
        onSave = { onSave(r.copy(name = r.name.trim())) },
        onClose = onClose,
        onDelete = if (isNew) null else ({ confirmDelete = true }),
    ) {
        Text(
            stringResource(R.string.role_examples),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = r.name,
            onValueChange = { r = r.copy(name = it) },
            label = { Text(stringResource(R.string.field_role_name)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        OutlinedTextField(
            value = r.description,
            onValueChange = { r = r.copy(description = it) },
            label = { Text(stringResource(R.string.field_role_desc)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        FieldLabel(stringResource(R.string.label_color))
        ColorPicker(selected = r.color, onSelect = { c -> if (c != null) r = r.copy(color = c) })
    }
    if (confirmDelete) {
        ConfirmDialog(stringResource(R.string.delete_role_confirm), stringResource(R.string.delete), onDelete) { confirmDelete = false }
    }
}

@Composable
private fun HabitEditor(initial: Habit, onSave: (Habit) -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    var h by remember { mutableStateOf(initial) }
    val isNew = initial.id == 0L
    var confirmDelete by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val askPermission = rememberNotificationPermission()
    EditorScaffold(
        title = stringResource(if (isNew) R.string.habit_new else R.string.habit_edit),
        canSave = h.name.isNotBlank(),
        onSave = { onSave(h.copy(name = h.name.trim())) },
        onClose = onClose,
        onDelete = if (isNew) null else ({ confirmDelete = true }),
    ) {
        OutlinedTextField(
            value = h.name,
            onValueChange = { h = h.copy(name = it) },
            label = { Text(stringResource(R.string.field_habit_name)) },
            placeholder = { Text(stringResource(R.string.habit_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        FieldLabel(stringResource(R.string.label_color))
        ColorPicker(selected = h.color, onSelect = { c -> if (c != null) h = h.copy(color = c) })
        FieldLabel(stringResource(R.string.label_reminder))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.habit_reminder_daily), Modifier.weight(1f))
            Switch(checked = h.reminderTime != null, onCheckedChange = { on ->
                if (on) askPermission()
                h = h.copy(reminderTime = if (on) 20 * 60 else null)
            })
        }
        val time = h.reminderTime
        if (time != null) {
            AssistChip(
                onClick = { pickTime = true },
                label = { Text(Dates.time(time)) },
                leadingIcon = { Icon(Icons.Rounded.Schedule, null, Modifier.size(18.dp)) },
            )
        }
    }
    if (pickTime) TimePickDialog(h.reminderTime, { pickTime = false }, { m -> h = h.copy(reminderTime = m) })
    if (confirmDelete) {
        ConfirmDialog(stringResource(R.string.delete_habit_confirm), stringResource(R.string.delete), onDelete) { confirmDelete = false }
    }
}

@Composable
private fun JournalEditor(initial: JournalEntry, onSave: (JournalEntry) -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf(initial.text) }
    val locale = currentLocale()
    var confirmDelete by remember { mutableStateOf(false) }
    val prompts = stringArrayResource(R.array.journal_prompts)
    EditorScaffold(
        title = Dates.format(LocalDate.ofEpochDay(initial.date), "EEEE, d MMMM", locale),
        canSave = true,
        onSave = { onSave(initial.copy(text = text.trim())) },
        onClose = onClose,
        onDelete = if (initial.id == 0L) null else ({ confirmDelete = true }),
    ) {
        FieldLabel(stringResource(R.string.journal_prompts_label))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            prompts.forEach { p ->
                SuggestionChip(
                    onClick = { text = if (text.isBlank()) "$p\n" else "${text.trimEnd()}\n\n$p\n" },
                    label = { Text(p) },
                )
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 10,
            placeholder = { Text(stringResource(R.string.journal_placeholder)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
    }
    if (confirmDelete) {
        ConfirmDialog(stringResource(R.string.delete_entry_confirm), stringResource(R.string.delete), onDelete) { confirmDelete = false }
    }
}
