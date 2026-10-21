@file:OptIn(ExperimentalMaterial3Api::class)

package ua.compass.planner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.compass.planner.R
import ua.compass.planner.ui.AppViewModel
import ua.compass.planner.ui.components.DrawerButton
import ua.compass.planner.ui.components.EmptyState
import ua.compass.planner.util.Dates
import ua.compass.planner.util.currentLocale
import java.time.LocalDate

@Composable
fun JournalScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val locale = currentLocale()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.journal_title)) }, navigationIcon = { DrawerButton() }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.openJournal(Dates.todayEpoch()) },
                icon = { Icon(Icons.Rounded.Edit, null) },
                text = { Text(stringResource(R.string.journal_today)) },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (data.journal.isEmpty()) {
                item { EmptyState(Icons.Rounded.Book, stringResource(R.string.journal_empty)) }
            }
            items(data.journal.sortedByDescending { it.date }, key = { it.id }) { entry ->
                Card(
                    onClick = { vm.editJournal(entry) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            Dates.format(LocalDate.ofEpochDay(entry.date), "EEEE, d MMMM yyyy", locale),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(entry.text, style = MaterialTheme.typography.bodyMedium, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
