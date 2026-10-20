package ua.compass.planner.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.compass.planner.CompassApp
import ua.compass.planner.data.AppData
import ua.compass.planner.data.Goal
import ua.compass.planner.data.Habit
import ua.compass.planner.data.JournalEntry
import ua.compass.planner.data.Palette
import ua.compass.planner.data.Quadrant
import ua.compass.planner.data.Role
import ua.compass.planner.data.Task
import ua.compass.planner.data.movedTo
import ua.compass.planner.util.Dates

sealed interface EditorRequest {
    data class TaskEdit(val task: Task) : EditorRequest
    data class GoalEdit(val goal: Goal, val week: Long) : EditorRequest
    data class RoleEdit(val role: Role) : EditorRequest
    data class HabitEdit(val habit: Habit) : EditorRequest
    data class JournalEdit(val entry: JournalEntry) : EditorRequest
}

/** Where a task ended up after drag and drop. */
data class Placement(val taskId: Long, val date: Long?, val order: Int)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CompassApp
    private val repo = app.repository

    val data: StateFlow<AppData> = repo.data
    val theme: StateFlow<String> = app.settings.theme
    val language: String get() = app.settings.language

    private val _editor = MutableStateFlow<EditorRequest?>(null)
    val editor: StateFlow<EditorRequest?> = _editor.asStateFlow()

    fun closeEditor() {
        _editor.value = null
    }

    // Tasks

    fun newTask(date: Long?, goal: Goal? = null, roleId: Long? = null, quadrant: Quadrant = Quadrant.II) {
        _editor.value = EditorRequest.TaskEdit(
            Task(
                date = date,
                goalId = goal?.id,
                roleId = goal?.roleId ?: roleId,
                important = quadrant.important,
                urgent = quadrant.urgent,
            )
        )
    }

    fun editTask(task: Task) {
        _editor.value = EditorRequest.TaskEdit(task)
    }

    fun saveTask(task: Task) {
        repo.update { d ->
            val old = d.tasks.firstOrNull { it.id == task.id }
            val nextOrder = (d.tasks.filter { it.date == task.date && it.id != task.id }.maxOfOrNull { it.order } ?: -1) + 1
            when {
                old == null -> d.copy(tasks = d.tasks + task.copy(id = repo.newId(), order = nextOrder))
                else -> d.copy(tasks = d.tasks.map {
                    if (it.id != task.id) it else if (old.date != task.date) task.copy(order = nextOrder) else task
                })
            }
        }
        closeEditor()
    }

    fun deleteTask(id: Long) {
        repo.update { d -> d.copy(tasks = d.tasks.filterNot { it.id == id }) }
        closeEditor()
    }

    fun toggleTask(id: Long) = updateTask(id) { it.copy(done = !it.done) }

    fun setQuadrant(id: Long, q: Quadrant) = updateTask(id) { it.copy(important = q.important, urgent = q.urgent) }

    fun applyPlacements(placements: List<Placement>) {
        val byId = placements.associateBy { it.taskId }
        repo.update { d ->
            d.copy(tasks = d.tasks.map { t -> byId[t.id]?.let { p -> t.movedTo(p.date).copy(order = p.order) } ?: t })
        }
    }

    fun moveOverdueToToday() {
        val today = Dates.todayEpoch()
        repo.update { d ->
            var order = (d.tasks.filter { it.date == today }.maxOfOrNull { it.order } ?: -1) + 1
            d.copy(tasks = d.tasks.map {
                val date = it.date
                if (!it.done && date != null && date < today) it.movedTo(today).copy(order = order++) else it
            })
        }
    }

    private fun updateTask(id: Long, transform: (Task) -> Task) {
        repo.update { d -> d.copy(tasks = d.tasks.map { if (it.id == id) transform(it) else it }) }
    }

    // Goals

    fun newGoal(roleId: Long?, week: Long, weekly: Boolean = true) {
        val color = data.value.roles.firstOrNull { it.id == roleId }?.color ?: Palette.DEFAULT
        _editor.value = EditorRequest.GoalEdit(
            Goal(roleId = roleId, color = color, weekStart = if (weekly) week else null),
            week,
        )
    }

    fun editGoal(goal: Goal, week: Long) {
        _editor.value = EditorRequest.GoalEdit(goal, week)
    }

    fun saveGoal(goal: Goal) {
        repo.update { d ->
            if (d.goals.any { it.id == goal.id }) d.copy(goals = d.goals.map { if (it.id == goal.id) goal else it })
            else d.copy(goals = d.goals + goal.copy(id = repo.newId()))
        }
        closeEditor()
    }

    fun deleteGoal(id: Long) {
        repo.update { d ->
            d.copy(
                goals = d.goals.filterNot { it.id == id },
                tasks = d.tasks.map { if (it.goalId == id) it.copy(goalId = null) else it },
            )
        }
        closeEditor()
    }

    fun toggleGoal(id: Long) {
        repo.update { d -> d.copy(goals = d.goals.map { if (it.id == id) it.copy(done = !it.done) else it }) }
    }

    // Roles

    fun newRole() {
        val roles = data.value.roles
        _editor.value = EditorRequest.RoleEdit(
            Role(color = Palette.colors[roles.size % Palette.colors.size], order = (roles.maxOfOrNull { it.order } ?: -1) + 1)
        )
    }

    fun editRole(role: Role) {
        _editor.value = EditorRequest.RoleEdit(role)
    }

    fun saveRole(role: Role) {
        repo.update { d ->
            if (d.roles.any { it.id == role.id }) d.copy(roles = d.roles.map { if (it.id == role.id) role else it })
            else d.copy(roles = d.roles + role.copy(id = repo.newId()))
        }
        closeEditor()
    }

    fun deleteRole(id: Long) {
        repo.update { d ->
            d.copy(
                roles = d.roles.filterNot { it.id == id },
                goals = d.goals.map { if (it.roleId == id) it.copy(roleId = null) else it },
                tasks = d.tasks.map { if (it.roleId == id) it.copy(roleId = null) else it },
            )
        }
        closeEditor()
    }

    // Habits

    fun newHabit() {
        val habits = data.value.habits
        _editor.value = EditorRequest.HabitEdit(
            Habit(color = Palette.colors[(habits.size + 2) % Palette.colors.size], order = habits.size)
        )
    }

    fun editHabit(habit: Habit) {
        _editor.value = EditorRequest.HabitEdit(habit)
    }

    fun saveHabit(habit: Habit) {
        repo.update { d ->
            if (d.habits.any { it.id == habit.id }) d.copy(habits = d.habits.map { if (it.id == habit.id) habit else it })
            else d.copy(habits = d.habits + habit.copy(id = repo.newId()))
        }
        closeEditor()
    }

    fun deleteHabit(id: Long) {
        repo.update { d -> d.copy(habits = d.habits.filterNot { it.id == id }) }
        closeEditor()
    }

    fun toggleHabit(id: Long, day: Long) {
        repo.update { d ->
            d.copy(habits = d.habits.map {
                if (it.id != id) it
                else if (day in it.doneDays) it.copy(doneDays = it.doneDays - day)
                else it.copy(doneDays = (it.doneDays + day).sorted())
            })
        }
    }

    // Journal

    fun openJournal(date: Long) {
        _editor.value = EditorRequest.JournalEdit(
            data.value.journal.firstOrNull { it.date == date } ?: JournalEntry(date = date)
        )
    }

    fun editJournal(entry: JournalEntry) {
        _editor.value = EditorRequest.JournalEdit(entry)
    }

    fun saveJournal(entry: JournalEntry) {
        repo.update { d ->
            when {
                entry.text.isBlank() -> d.copy(journal = d.journal.filterNot { it.id == entry.id })
                d.journal.any { it.id == entry.id } -> d.copy(journal = d.journal.map { if (it.id == entry.id) entry else it })
                else -> d.copy(journal = d.journal + entry.copy(id = repo.newId()))
            }
        }
        closeEditor()
    }

    fun deleteJournal(id: Long) {
        repo.update { d -> d.copy(journal = d.journal.filterNot { it.id == id }) }
        closeEditor()
    }

    // Mission and settings

    fun setMission(text: String) {
        if (text != data.value.mission) repo.update { it.copy(mission = text) }
    }

    fun setTheme(value: String) = app.settings.setTheme(value)

    fun setLanguage(value: String) = app.settings.setLanguage(value)

    fun exportTo(uri: Uri): Boolean = try {
        app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(repo.exportJson().toByteArray()) } != null
    } catch (e: Exception) {
        false
    }

    fun importFrom(uri: Uri): Boolean = try {
        val text = app.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        text != null && repo.importJson(text)
    } catch (e: Exception) {
        false
    }
}
