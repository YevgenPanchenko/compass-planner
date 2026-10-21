@file:OptIn(ExperimentalMaterial3Api::class)

package ua.compass.planner.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ua.compass.planner.BuildConfig
import ua.compass.planner.R
import ua.compass.planner.ui.AppViewModel
import ua.compass.planner.ui.components.ColorDot
import ua.compass.planner.ui.components.ConfirmDialog
import ua.compass.planner.ui.components.EmptyState
import ua.compass.planner.ui.components.SectionCard
import ua.compass.planner.ui.components.SubScaffold
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun MissionScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    var text by rememberSaveable { mutableStateOf(data.mission) }
    val latest by rememberUpdatedState(text)
    DisposableEffect(Unit) { onDispose { vm.setMission(latest.trim()) } }
    SubScaffold(title = stringResource(R.string.mission_title)) { pad ->
        Column(
            Modifier
                .padding(pad)
                .consumeWindowInsets(pad)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard {
                Text(stringResource(R.string.mission_intro), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.mission_questions),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
                placeholder = { Text(stringResource(R.string.mission_placeholder)) },
            )
            Text(
                stringResource(R.string.autosave_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RolesScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    SubScaffold(
        title = stringResource(R.string.roles_title),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.newRole() },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.add_role)) },
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
            item {
                Text(
                    stringResource(R.string.roles_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (data.roles.isEmpty()) item { EmptyState(Icons.Rounded.Groups, stringResource(R.string.roles_empty)) }
            items(data.roles.sortedBy { it.order }, key = { it.id }) { role ->
                val goals = data.goals.filter { it.roleId == role.id }
                Card(
                    onClick = { vm.editRole(role) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(Color(role.color), 14)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(role.name, style = MaterialTheme.typography.titleMedium)
                            if (role.description.isNotBlank()) {
                                Text(
                                    role.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                stringResource(R.string.role_goals_count, goals.count { !it.done }, goals.count { it.done }),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Icon(Icons.Rounded.Edit, stringResource(R.string.edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun GuideScreen() {
    val habits = listOf(
        R.string.habit1_title to R.string.habit1_text,
        R.string.habit2_title to R.string.habit2_text,
        R.string.habit3_title to R.string.habit3_text,
        R.string.habit4_title to R.string.habit4_text,
        R.string.habit5_title to R.string.habit5_text,
        R.string.habit6_title to R.string.habit6_text,
        R.string.habit7_title to R.string.habit7_text,
    )
    SubScaffold(title = stringResource(R.string.guide_title)) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(habits.size) { i ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                "${i + 1}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(habits[i].first), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(habits[i].second), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                SectionCard {
                    Text(stringResource(R.string.guide_app_title), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.guide_app_text), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val context = LocalContext.current
    val theme by vm.theme.collectAsStateWithLifecycle()
    val firstDay by vm.firstDay.collectAsStateWithLifecycle()
    var confirmImport by remember { mutableStateOf(false) }
    val exportOk = stringResource(R.string.export_ok)
    val importOk = stringResource(R.string.import_ok)
    val failed = stringResource(R.string.file_error)
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) Toast.makeText(context, if (vm.exportTo(uri)) exportOk else failed, Toast.LENGTH_SHORT).show()
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) Toast.makeText(context, if (vm.importFrom(uri)) importOk else failed, Toast.LENGTH_SHORT).show()
    }

    SubScaffold(title = stringResource(R.string.settings_title)) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                listOf(
                    "system" to R.string.theme_system,
                    "light" to R.string.theme_light,
                    "dark" to R.string.theme_dark,
                ).forEach { (value, label) ->
                    RadioRow(stringResource(label), theme == value) { vm.setTheme(value) }
                }
            }
            SectionCard {
                Text(stringResource(R.string.settings_first_day), style = MaterialTheme.typography.titleMedium)
                listOf(DayOfWeek.MONDAY to R.string.monday, DayOfWeek.SUNDAY to R.string.sunday).forEach { (day, label) ->
                    RadioRow(stringResource(label), firstDay == day) { vm.setFirstDay(day) }
                }
            }
            SectionCard {
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
                val current = vm.language
                listOf(
                    "system" to stringResource(R.string.language_system),
                    "uk" to "Українська",
                    "en" to "English",
                ).forEach { (value, label) ->
                    RadioRow(label, current == value) {
                        if (current != value) {
                            vm.setLanguage(value)
                            (context as? Activity)?.recreate()
                        }
                    }
                }
            }
            SectionCard {
                Text(stringResource(R.string.settings_data), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.settings_data_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exporter.launch("compass-backup-${LocalDate.now()}.json") }) {
                        Icon(Icons.Rounded.FileUpload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.export))
                    }
                    OutlinedButton(onClick = { confirmImport = true }) {
                        Icon(Icons.Rounded.FileDownload, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.import_data))
                    }
                }
            }
            Text(
                stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
    if (confirmImport) {
        ConfirmDialog(
            text = stringResource(R.string.import_confirm),
            confirm = stringResource(R.string.import_data),
            onConfirm = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            onDismiss = { confirmImport = false },
        )
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}
