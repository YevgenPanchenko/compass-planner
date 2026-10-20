@file:OptIn(ExperimentalMaterial3Api::class)

package ua.compass.planner.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.compass.planner.R
import ua.compass.planner.data.AppData
import ua.compass.planner.data.isDone
import ua.compass.planner.ui.AppViewModel
import ua.compass.planner.ui.components.ColorDot
import ua.compass.planner.ui.components.GoalLine
import ua.compass.planner.ui.components.SectionCard
import ua.compass.planner.ui.components.TaskCard
import ua.compass.planner.ui.components.taskColor
import ua.compass.planner.ui.components.taskMeta
import ua.compass.planner.util.Dates
import ua.compass.planner.util.currentLocale
import java.time.LocalDate
import java.time.YearMonth

enum class PlannerMode(val label: Int) {
    DAY(R.string.mode_day), WEEK(R.string.mode_week), MONTH(R.string.mode_month)
}

@Composable
fun PlannerScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val locale = currentLocale()
    var mode by rememberSaveable { mutableStateOf(PlannerMode.WEEK) }
    var anchorDay by rememberSaveable { mutableLongStateOf(Dates.todayEpoch()) }
    val anchor = LocalDate.ofEpochDay(anchorDay)
    val today = LocalDate.now()
    val weekStart = Dates.weekStart(anchor)

    val title = when (mode) {
        PlannerMode.DAY -> Dates.format(anchor, "EEEE, d MMMM", locale)
        PlannerMode.WEEK -> Dates.weekRange(weekStart, locale)
        PlannerMode.MONTH -> Dates.format(anchor, "LLLL yyyy", locale)
    }

    fun shift(step: Long) {
        anchorDay = when (mode) {
            PlannerMode.DAY -> anchor.plusDays(step)
            PlannerMode.WEEK -> anchor.plusWeeks(step)
            PlannerMode.MONTH -> anchor.plusMonths(step)
        }.toEpochDay()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = { shift(-1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous))
                    }
                    IconButton(onClick = { anchorDay = Dates.todayEpoch() }) {
                        Icon(Icons.Rounded.Today, stringResource(R.string.today))
                    }
                    IconButton(onClick = { shift(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val inWeek = !today.isBefore(weekStart) && !today.isAfter(weekStart.plusDays(6))
                vm.newTask(
                    when (mode) {
                        PlannerMode.WEEK -> if (inWeek) today.toEpochDay() else weekStart.toEpochDay()
                        else -> anchorDay
                    }
                )
            }) {
                Icon(Icons.Rounded.Add, stringResource(R.string.add_task))
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
                PlannerMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(i, PlannerMode.entries.size),
                    ) { Text(stringResource(m.label)) }
                }
            }
            when (mode) {
                PlannerMode.WEEK -> WeekPlanner(vm, data, weekStart) { day ->
                    anchorDay = day.toEpochDay()
                    mode = PlannerMode.DAY
                }
                PlannerMode.DAY -> DayPlanner(vm, data, anchor)
                PlannerMode.MONTH -> MonthView(
                    vm, data, anchor,
                    onSelect = { anchorDay = it.toEpochDay() },
                    onOpenDay = {
                        anchorDay = it.toEpochDay()
                        mode = PlannerMode.DAY
                    },
                )
            }
        }
    }
}

@Composable
private fun WeekPlanner(vm: AppViewModel, data: AppData, weekStart: LocalDate, onOpenDay: (LocalDate) -> Unit) {
    val locale = currentLocale()
    val today = LocalDate.now()
    val inboxTitle = stringResource(R.string.inbox)
    val sections = remember(weekStart, locale, inboxTitle, today) {
        (0L..6L).map { i ->
            val d = weekStart.plusDays(i)
            Section("d${d.toEpochDay()}", d.toEpochDay(), Dates.format(d, "EEEE, d MMM", locale), d == today)
        } + Section("inbox", null, inboxTitle, false)
    }
    val isCurrentWeek = Dates.weekStart(today) == weekStart
    val overdue = remember(data.tasks, today) { overdueCount(data, today) }
    PlannerList(
        vm = vm,
        data = data,
        sections = sections,
        onHeaderClick = { s -> s.date?.let { onOpenDay(LocalDate.ofEpochDay(it)) } },
        prefix = {
            if (data.mission.isNotBlank()) item(key = "mission") { MissionQuote(data.mission) }
            if (isCurrentWeek && overdue > 0) item(key = "overdue") { OverdueBanner(overdue) { vm.moveOverdueToToday() } }
            item(key = "goals") { WeekGoalsCard(vm, data, weekStart.toEpochDay()) }
        },
    )
}

@Composable
private fun DayPlanner(vm: AppViewModel, data: AppData, day: LocalDate) {
    val today = LocalDate.now()
    val title = stringResource(R.string.day_tasks)
    val sections = remember(day, title, today) {
        listOf(Section("d${day.toEpochDay()}", day.toEpochDay(), title, day == today))
    }
    val overdue = remember(data.tasks, today) { overdueCount(data, today) }
    PlannerList(
        vm = vm,
        data = data,
        sections = sections,
        onHeaderClick = {},
        prefix = {
            if (day == today && overdue > 0) item(key = "overdue") { OverdueBanner(overdue) { vm.moveOverdueToToday() } }
            if (data.habits.isNotEmpty()) item(key = "habits") { HabitsStrip(vm, data, day.toEpochDay()) }
        },
    )
}

private fun overdueCount(data: AppData, today: LocalDate): Int {
    val t = today.toEpochDay()
    return data.tasks.count { task -> !task.done && task.date.let { it != null && it < t } }
}

@Composable
private fun MissionQuote(mission: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Explore, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                mission,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OverdueBanner(count: Int, onMove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.overdue_count, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onMove) { Text(stringResource(R.string.move_to_today)) }
        }
    }
}

@Composable
private fun WeekGoalsCard(vm: AppViewModel, data: AppData, weekStart: Long) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val goals = data.goals.filter { it.weekStart == weekStart || (it.weekStart == null && !it.done) }
    val weekly = goals.filter { it.weekStart != null }
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
            Text(stringResource(R.string.week_goals), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (weekly.isNotEmpty()) {
                Text(
                    "${weekly.count { it.done }}/${weekly.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
            }
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
        }
        if (!expanded) return@SectionCard
        Spacer(Modifier.height(4.dp))
        if (data.roles.isEmpty()) {
            Text(
                stringResource(R.string.week_goals_no_roles),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { vm.newRole() }) { Text(stringResource(R.string.add_role)) }
        }
        val roleIds = data.roles.map { it.id }.toSet()
        data.roles.sortedBy { it.order }.forEach { role ->
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                ColorDot(Color(role.color), 10)
                Spacer(Modifier.width(8.dp))
                Text(
                    role.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { vm.newGoal(role.id, weekStart) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.add_goal), Modifier.size(20.dp))
                }
            }
            goals.filter { it.roleId == role.id }.forEach { goal -> WeekGoal(vm, goal, weekStart) }
        }
        goals.filter { it.roleId == null || it.roleId !in roleIds }.takeIf { it.isNotEmpty() }?.let { orphans ->
            Text(
                stringResource(R.string.no_role),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 6.dp),
            )
            orphans.forEach { goal -> WeekGoal(vm, goal, weekStart) }
        }
        if (data.roles.isNotEmpty()) {
            Text(
                stringResource(R.string.week_goals_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun WeekGoal(vm: AppViewModel, goal: ua.compass.planner.data.Goal, weekStart: Long) {
    GoalLine(
        goal = goal,
        onToggle = { vm.toggleGoal(goal.id) },
        onClick = { vm.editGoal(goal, weekStart) },
        trailing = {
            IconButton(onClick = { vm.newTask(null, goal = goal) }) {
                Icon(Icons.Rounded.AddTask, stringResource(R.string.add_task_for_goal), Modifier.size(20.dp))
            }
        },
    )
}

@Composable
private fun HabitsStrip(vm: AppViewModel, data: AppData, day: Long) {
    Column {
        Text(
            stringResource(R.string.habits_for_day),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            data.habits.sortedBy { it.order }.forEach { habit ->
                val done = habit.isDone(day)
                FilterChip(
                    selected = done,
                    onClick = { vm.toggleHabit(habit.id, day) },
                    label = { Text(habit.name) },
                    leadingIcon = {
                        if (done) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp))
                        else ColorDot(Color(habit.color), 10)
                    },
                )
            }
        }
    }
}

@Composable
private fun MonthView(
    vm: AppViewModel,
    data: AppData,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val locale = currentLocale()
    val today = LocalDate.now()
    val month = YearMonth.from(selected)
    val first = month.atDay(1)
    val gridStart = Dates.weekStart(first)
    val weeks = (first.dayOfWeek.value - 1 + month.lengthOfMonth() + 6) / 7
    val byDay = remember(data.tasks) { data.tasks.filter { it.date != null }.groupBy { it.date!! } }
    val roles = remember(data.roles) { data.roles.associateBy { it.id } }
    val goals = remember(data.goals) { data.goals.associateBy { it.id } }
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 96.dp)
    ) {
        Row(Modifier.padding(vertical = 6.dp)) {
            (0L..6L).forEach { i ->
                Text(
                    Dates.format(gridStart.plusDays(i), "EE", locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        for (w in 0 until weeks) {
            Row {
                for (i in 0..6) {
                    val d = gridStart.plusDays((w * 7 + i).toLong())
                    val tasks = byDay[d.toEpochDay()].orEmpty()
                    val isSelected = d == selected
                    val isToday = d == today
                    Column(
                        Modifier
                            .weight(1f)
                            .height(62.dp)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) scheme.primaryContainer else Color.Transparent)
                            .clickable { if (isSelected) onOpenDay(d) else onSelect(d) }
                            .padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(if (isToday) scheme.primary else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                d.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isToday -> scheme.onPrimary
                                    d.month == month.month -> scheme.onSurface
                                    else -> scheme.onSurfaceVariant.copy(alpha = 0.45f)
                                },
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            tasks.sortedBy { it.order }.take(3).forEach { t ->
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(taskColor(t, roles, goals).copy(alpha = if (t.done) 0.35f else 1f), CircleShape)
                                )
                            }
                            if (tasks.size > 3) {
                                Text("+${tasks.size - 3}", fontSize = 9.sp, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                Dates.format(selected, "EEEE, d MMMM", locale),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onOpenDay(selected) }) { Text(stringResource(R.string.open_day)) }
        }
        val dayTasks = byDay[selected.toEpochDay()].orEmpty().sortedWith(compareBy({ it.order }, { it.id }))
        if (dayTasks.isEmpty()) {
            Text(
                stringResource(R.string.day_empty_simple),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dayTasks.forEach { t ->
                TaskCard(
                    task = t,
                    color = taskColor(t, roles, goals),
                    meta = taskMeta(t, roles, goals),
                    onToggle = { vm.toggleTask(t.id) },
                    onClick = { vm.editTask(t) },
                    showHandle = false,
                )
            }
        }
    }
}
