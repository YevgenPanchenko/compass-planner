@file:OptIn(ExperimentalMaterial3Api::class)

package ua.compass.planner.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.compass.planner.R
import ua.compass.planner.data.Habit
import ua.compass.planner.data.isDone
import ua.compass.planner.data.streak
import ua.compass.planner.ui.AppViewModel
import ua.compass.planner.ui.components.ColorDot
import ua.compass.planner.ui.components.EmptyState
import ua.compass.planner.util.Dates
import ua.compass.planner.util.currentLocale
import java.time.LocalDate

@Composable
fun HabitsScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val today = Dates.todayEpoch()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.habits_title)) }) },
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
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        onClick = onEdit,
    ) {
        Column(Modifier.padding(16.dp)) {
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
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (today - 6..today).forEach { day ->
                    val date = LocalDate.ofEpochDay(day)
                    val done = habit.isDone(day)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            Dates.format(date, "EE", locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day == today) scheme.primary else scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (done) color else Color.Transparent)
                                .border(2.dp, if (done) color else scheme.outlineVariant, CircleShape)
                                .clickable { onToggle(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            else Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium)
                        }
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
            Text(
                stringResource(R.string.habit_last30, last30),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
