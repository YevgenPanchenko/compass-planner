package ua.compass.planner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import ua.compass.planner.R
import ua.compass.planner.ui.editors.EditorHost
import ua.compass.planner.ui.planner.PlannerScreen
import ua.compass.planner.ui.screens.HabitsScreen
import ua.compass.planner.ui.screens.JournalScreen
import ua.compass.planner.ui.screens.MatrixScreen
import ua.compass.planner.ui.screens.MoreRoute
import ua.compass.planner.ui.screens.MoreScreen

private enum class Tab(val label: Int, val icon: ImageVector) {
    PLANNER(R.string.tab_planner, Icons.Rounded.CalendarMonth),
    MATRIX(R.string.tab_matrix, Icons.Rounded.GridView),
    HABITS(R.string.tab_habits, Icons.Rounded.TaskAlt),
    JOURNAL(R.string.tab_journal, Icons.Rounded.Book),
    MORE(R.string.tab_more, Icons.Rounded.MoreHoriz),
}

@Composable
fun AppRoot(vm: AppViewModel) {
    Box(Modifier.fillMaxSize()) {
        MainScreen(vm)
        EditorHost(vm)
    }
}

@Composable
private fun MainScreen(vm: AppViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.PLANNER) }
    var moreRoute by rememberSaveable { mutableStateOf<MoreRoute?>(null) }
    val holder = rememberSaveableStateHolder()

    BackHandler(enabled = tab != Tab.PLANNER || moreRoute != null) {
        if (moreRoute != null) moreRoute = null else tab = Tab.PLANNER
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = {
                            if (t == Tab.MORE && tab == Tab.MORE) moreRoute = null
                            tab = t
                        },
                        icon = { Icon(t.icon, null) },
                        label = { Text(stringResource(t.label), maxLines = 1) },
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
            holder.SaveableStateProvider(tab.name) {
                when (tab) {
                    Tab.PLANNER -> PlannerScreen(vm)
                    Tab.MATRIX -> MatrixScreen(vm)
                    Tab.HABITS -> HabitsScreen(vm)
                    Tab.JOURNAL -> JournalScreen(vm)
                    Tab.MORE -> MoreScreen(vm, moreRoute) { moreRoute = it }
                }
            }
        }
    }
}
