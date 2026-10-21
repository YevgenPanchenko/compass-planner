@file:OptIn(ExperimentalMaterial3Api::class)

package ua.compass.planner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.compass.planner.R
import ua.compass.planner.data.Habit
import ua.compass.planner.data.isDone
import ua.compass.planner.data.streak
import ua.compass.planner.ui.AppViewModel
import ua.compass.planner.ui.components.DrawerButton
import ua.compass.planner.ui.components.ColorDot
import ua.compass.planner.ui.components.EmptyState
import ua.compass.planner.ui.components.PeriodNavigator
import ua.compass.planner.util.Dates
import ua.compass.planner.util.LocalFirstDayOfWeek
import ua.compass.planner.util.currentLocale
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HabitsScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val today = Dates.todayEpoch()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.habits_title)) }, navigationIcon = { DrawerButton() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.newHabit() }) { Icon(Icons.Rounded.Add, stringResource(R.string.add_habit)) }
        },
    ) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.habits_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (data.habits.isEmpty()) {
                item { EmptyState(Icons.Rounded.TaskAlt, stringResource(R.string.habits_empty)) }
            }
            items(data.habits.sortedBy { it.order }, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    today = today,
                    onToggle = { day -> vm.toggleHabit(habit.id, day) },
                    onEdit = { vm.editHabit(habit) },
                )
            }
        }
    }
}

@Composable
private fun HabitCard(habit: Habit, today: Long, onToggle: (Long) -> Unit, onEdit: () -> Unit) {
    val locale = currentLocale()
    val scheme = MaterialTheme.colorScheme
    val color = Color(habit.color)
    val streak = habit.streak(today)
    val last30 = (today - 29..today).count { habit.isDone(it) }
    var expanded by rememberSaveable(habit.id) { mutableStateOf(false) }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        onClick = { expanded = !expanded },
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(color, 12)
                Spacer(Modifier.width(10.dp))
                Text(habit.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (habit.reminderTime != null) {
                    Icon(Icons.Rounded.Notifications, null, Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                    Text(
                        Dates.time(habit.reminderTime),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, end = 10.dp),
                    )
                }
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                Text(streak.toString(), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, stringResource(R.string.edit), Modifier.size(20.dp), tint = scheme.onSurfaceVariant)
                }
            }
            Column(Modifier.padding(end = 8.dp)) {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (today - 6..today).forEach { day ->
                        val date = LocalDate.ofEpochDay(day)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                Dates.format(date, "EE", locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (day == today) scheme.primary else scheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            DayCircle(date.dayOfMonth, habit.isDone(day), color, isToday = false, enabled = true) { onToggle(day) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { last30 / 30f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50)),
                    color = color,
                    trackColor = scheme.surfaceContainerHighest,
                    drawStopIndicator = {},
                )
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.habit_last30, last30),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(if (expanded) R.string.habit_hide_month else R.string.habit_show_month),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary,
                    )
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null,
                        Modifier.size(18.dp),
                        tint = scheme.primary,
                    )
                }
                AnimatedVisibility(expanded) {
                    HabitMonth(habit, today, color, onToggle)
                }
            }
        }
    }
}

@Composable
private fun DayCircle(day: Int, done: Boolean, color: Color, isToday: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (done) color else Color.Transparent)
            .border(
                2.dp,
                when {
                    done -> color
                    isToday -> scheme.primary
                    else -> scheme.outlineVariant
                },
                CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
        else Text(
            day.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.35f),
        )
    }
}

/** Month calendar where any past day can be marked. */
@Composable
private fun HabitMonth(habit: Habit, today: Long, color: Color, onToggle: (Long) -> Unit) {
    val locale = currentLocale()
    val firstDay = LocalFirstDayOfWeek.current
    val currentMonth = Dates.monthIndex(YearMonth.now())
    var monthIndex by rememberSaveable(habit.id) { mutableIntStateOf(currentMonth) }
    val month = Dates.yearMonth(monthIndex)
    val first = month.atDay(1)
    val gridStart = Dates.weekStart(first, firstDay)
    val lead = (first.toEpochDay() - gridStart.toEpochDay()).toInt()
    val weeks = (lead + month.lengthOfMonth() + 6) / 7
    val doneInMonth = (1..month.lengthOfMonth()).count { habit.isDone(month.atDay(it).toEpochDay()) }

    Column(Modifier.padding(top = 8.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PeriodNavigator(
            Dates.format(first, "LLLL yyyy", locale),
            onPrev = { monthIndex-- },
            onNext = { if (monthIndex < currentMonth) monthIndex++ },
        )
        Row(Modifier.fillMaxWidth()) {
            (0L..6L).forEach { i ->
                Text(
                    Dates.format(gridStart.plusDays(i), "EE", locale),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (w in 0 until weeks) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0..6) {
                    val date = gridStart.plusDays((w * 7 + i).toLong())
                    val day = date.toEpochDay()
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date.month == month.month) {
                            DayCircle(
                                date.dayOfMonth,
                                habit.isDone(day),
                                color,
                                isToday = day == today,
                                enabled = day <= today,
                            ) { onToggle(day) }
                        }
                    }
                }
            }
        }
        Text(
            stringResource(R.string.habit_month_count, doneInMonth, month.lengthOfMonth()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
