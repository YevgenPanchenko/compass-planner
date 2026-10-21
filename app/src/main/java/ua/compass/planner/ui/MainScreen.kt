package ua.compass.planner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ua.compass.planner.R
import ua.compass.planner.ui.components.LocalOpenDrawer
import ua.compass.planner.ui.editors.EditorHost
import ua.compass.planner.ui.planner.PlannerScreen
import ua.compass.planner.ui.screens.GoalsScreen
import ua.compass.planner.ui.screens.GuideScreen
import ua.compass.planner.ui.screens.HabitsScreen
import ua.compass.planner.ui.screens.JournalScreen
import ua.compass.planner.ui.screens.MatrixScreen
import ua.compass.planner.ui.screens.MissionScreen
import ua.compass.planner.ui.screens.RolesScreen
import ua.compass.planner.ui.screens.SettingsScreen

private enum class Destination(val label: Int, val icon: ImageVector, val inBottomBar: Boolean) {
    CALENDAR(R.string.nav_calendar, Icons.Rounded.CalendarMonth, true),
    GOALS(R.string.nav_goals, Icons.Rounded.Flag, true),
    MATRIX(R.string.nav_matrix, Icons.Rounded.GridView, true),
    HABITS(R.string.nav_habits, Icons.Rounded.TaskAlt, true),
    JOURNAL(R.string.nav_journal, Icons.Rounded.Book, true),
    MISSION(R.string.nav_mission, Icons.Rounded.Explore, false),
    ROLES(R.string.nav_roles, Icons.Rounded.Groups, false),
    GUIDE(R.string.nav_guide, Icons.Rounded.Lightbulb, false),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings, false),
}

private val drawerGroups = listOf(
    listOf(Destination.CALENDAR, Destination.GOALS, Destination.MATRIX, Destination.HABITS, Destination.JOURNAL),
    listOf(Destination.MISSION, Destination.ROLES, Destination.GUIDE),
    listOf(Destination.SETTINGS),
)

@Composable
fun AppRoot(vm: AppViewModel) {
    Box(Modifier.fillMaxSize()) {
        MainScreen(vm)
        EditorHost(vm)
    }
}

@Composable
private fun MainScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    var dest by rememberSaveable { mutableStateOf(Destination.CALENDAR) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val holder = rememberSaveableStateHolder()

    BackHandler(enabled = !drawerState.isOpen && dest != Destination.CALENDAR) { dest = Destination.CALENDAR }
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Column(Modifier.padding(start = 28.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)) {
                        Icon(
                            Icons.Rounded.Explore,
                            null,
                            Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        if (data.mission.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                data.mission,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    drawerGroups.forEachIndexed { i, group ->
                        if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
                        group.forEach { d ->
                            NavigationDrawerItem(
                                label = { Text(stringResource(d.label)) },
                                icon = { Icon(d.icon, null) },
                                selected = dest == d,
                                onClick = {
                                    dest = d
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        },
    ) {
        CompositionLocalProvider(LocalOpenDrawer provides { scope.launch { drawerState.open() } }) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar {
                        Destination.entries.filter { it.inBottomBar }.forEach { d ->
                            NavigationBarItem(
                                selected = dest == d,
                                onClick = { dest = d },
                                icon = { Icon(d.icon, null) },
                                label = { Text(stringResource(d.label), maxLines = 1) },
                            )
                        }
                    }
                },
            ) { pad ->
                Box(
                    Modifier
                        .padding(pad)
                        .fillMaxSize()
                ) {
                    holder.SaveableStateProvider(dest.name) {
                        when (dest) {
                            Destination.CALENDAR -> PlannerScreen(vm)
                            Destination.GOALS -> GoalsScreen(vm)
                            Destination.MATRIX -> MatrixScreen(vm)
                            Destination.HABITS -> HabitsScreen(vm)
                            Destination.JOURNAL -> JournalScreen(vm)
                            Destination.MISSION -> MissionScreen(vm)
                            Destination.ROLES -> RolesScreen(vm)
                            Destination.GUIDE -> GuideScreen()
                            Destination.SETTINGS -> SettingsScreen(vm)
                        }
                    }
                }
            }
        }
    }
}
