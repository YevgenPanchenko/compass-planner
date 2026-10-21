@file:OptIn(ExperimentalMaterial3Api::class)

package ua.compass.planner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.compass.planner.R
import ua.compass.planner.data.Goal
import ua.compass.planner.data.Horizon
import ua.compass.planner.data.Task
import ua.compass.planner.data.horizon
import ua.compass.planner.data.inWeek
import ua.compass.planner.ui.AppViewModel
import ua.compass.planner.ui.components.DrawerButton
import ua.compass.planner.ui.components.EmptyState
import ua.compass.planner.ui.components.PeriodNavigator
import ua.compass.planner.ui.components.goalTasksLabel
import ua.compass.planner.util.Dates
import ua.compass.planner.util.LocalFirstDayOfWeek
import ua.compass.planner.util.currentLocale
import java.time.LocalDate
import java.time.YearMonth

private val Horizon.tab: Int
    get() = when (this) {
        Horizon.WEEK -> R.string.horizon_week
        Horizon.MONTH -> R.string.horizon_month
        Horizon.LONG -> R.string.horizon_long
    }

@Composable
fun GoalsScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val locale = currentLocale()
    val firstDay = LocalFirstDayOfWeek.current
    var horizon by rememberSaveable { mutableStateOf(Horizon.WEEK) }
    var refDay by rememberSaveable { mutableLongStateOf(Dates.todayEpoch()) }
    val ref = LocalDate.ofEpochDay(refDay)
    val weekStart = Dates.weekStart(ref, firstDay)
    val month = YearMonth.from(ref)
    val monthIndex = Dates.monthIndex(month)
    val roles = remember(data.roles) { data.roles.associateBy { it.id } }

    val goals = when (horizon) {
        Horizon.WEEK -> data.goals.filter { it.inWeek(weekStart, firstDay) }
        Horizon.MONTH -> data.goals.filter { it.month == monthIndex }
        Horizon.LONG -> data.goals.filter { it.horizon == Horizon.LONG }
    }.sortedBy { it.done }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.goals_title)) },
                navigationIcon = { DrawerButton() },
                actions = {
                    if (horizon != Horizon.LONG) {
                        IconButton(onClick = { refDay = Dates.todayEpoch() }) {
                            Icon(Icons.Rounded.Today, stringResource(R.string.today))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.newGoal(horizon, refDay) }) {
                Icon(Icons.Rounded.Add, stringResource(R.string.add_goal))
            }
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Horizon.entries.forEachIndexed { i, h ->
                    SegmentedButton(
                        selected = horizon == h,
                        onClick = { horizon = h },
                        shape = SegmentedButtonDefaults.itemShape(i, Horizon.entries.size),
                    ) { Text(stringResource(h.tab), maxLines = 1) }
                }
            }
            when (horizon) {
                Horizon.WEEK -> PeriodNavigator(
                    Dates.weekRange(weekStart, locale),
                    onPrev = { refDay -= 7 },
                    onNext = { refDay += 7 },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Horizon.MONTH -> PeriodNavigator(
                    Dates.format(month.atDay(1), "LLLL yyyy", locale),
                    onPrev = { refDay = month.minusMonths(1).atDay(1).toEpochDay() },
                    onNext = { refDay = month.plusMonths(1).atDay(1).toEpochDay() },
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Horizon.LONG -> Unit
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.goals_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (goals.isEmpty()) {
                    item { EmptyState(Icons.Rounded.Flag, stringResource(R.string.goals_empty)) }
                }
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        tasks = data.tasks.filter { it.goalId == goal.id },
                        roleName = goal.roleId?.let { roles[it]?.name },
                        vm = vm,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalCard(goal: Goal, tasks: List<Task>, roleName: String?, vm: AppViewModel) {
    var expanded by rememberSaveable(goal.id) { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val color = Color(goal.color)
    Card(
        onClick = { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = goal.done,
                        onCheckedChange = { vm.toggleGoal(goal.id) },
                        colors = CheckboxDefaults.colors(checkedColor = color, uncheckedColor = color),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            goal.title,
                            style = MaterialTheme.typography.titleSmall,
                            textDecoration = if (goal.done) TextDecoration.LineThrough else null,
                            color = if (goal.done) scheme.onSurfaceVariant else scheme.onSurface,
                        )
                        Text(
                            listOfNotNull(roleName, goalTasksLabel(goal, tasks)).joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { vm.editGoal(goal) }) {
                        Icon(Icons.Rounded.Edit, stringResource(R.string.edit), Modifier.size(20.dp))
                    }
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null,
                        Modifier.padding(end = 8.dp),
                    )
                }
                if (tasks.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { tasks.count { it.done } / tasks.size.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(50)),
                        color = color,
                        trackColor = scheme.surfaceContainerHighest,
                        drawStopIndicator = {},
                    )
                }
                AnimatedVisibility(expanded) {
                    Column(Modifier.padding(start = 4.dp, end = 12.dp)) {
                        if (goal.notes.isNotBlank()) {
                            Text(
                                goal.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 10.dp, bottom = 4.dp),
                            )
                        }
                        tasks.sortedWith(compareBy<Task>({ it.done }, { it.date ?: Long.MAX_VALUE }, { it.order }))
                            .forEach { task -> GoalTaskRow(task, color, vm) }
                        TextButton(onClick = { vm.newTaskForGoal(goal) }) {
                            Icon(Icons.Rounded.AddTask, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.add_task))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalTaskRow(task: Task, color: Color, vm: AppViewModel) {
    val locale = currentLocale()
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { vm.editTask(task) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.done,
            onCheckedChange = { vm.toggleTask(task.id) },
            colors = CheckboxDefaults.colors(checkedColor = color, uncheckedColor = color),
        )
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                color = if (task.done) scheme.onSurfaceVariant else scheme.onSurface,
            )
            val date = task.date
            Text(
                listOfNotNull(
                    if (date == null) stringResource(R.string.inbox)
                    else Dates.format(LocalDate.ofEpochDay(date), "EE, d MMM", locale),
                    task.time?.let(Dates::time),
                ).joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
