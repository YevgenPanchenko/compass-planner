package ua.compass.planner.data

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

object Palette {
    const val DEFAULT = 0xFF3390EC
    val colors = listOf(
        0xFF3390EC, 0xFF4FC3F7, 0xFF26A69A, 0xFF66BB6A,
        0xFFC0CA33, 0xFFFFB300, 0xFFFF7043, 0xFFEF5350,
        0xFFEC407A, 0xFFAB47BC, 0xFF5C6BC0, 0xFF78909C,
    )
}

@Serializable
data class Role(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val color: Long = Palette.DEFAULT,
    val order: Int = 0,
)

@Serializable
data class Goal(
    val id: Long = 0,
    val title: String = "",
    val notes: String = "",
    val roleId: Long? = null,
    val color: Long = Palette.DEFAULT,
    /** Epoch day of the week's Monday; null means a long-term goal. */
    val weekStart: Long? = null,
    val done: Boolean = false,
    val reminderAt: Long? = null,
)

@Serializable
data class Task(
    val id: Long = 0,
    val title: String = "",
    val notes: String = "",
    /** Epoch day; null means the task sits in the inbox. */
    val date: Long? = null,
    /** Minutes since midnight. */
    val time: Int? = null,
    val roleId: Long? = null,
    val goalId: Long? = null,
    /** Explicit color; null inherits from the goal or role. */
    val color: Long? = null,
    val important: Boolean = true,
    val urgent: Boolean = false,
    val done: Boolean = false,
    val order: Int = 0,
    val reminderAt: Long? = null,
)

@Serializable
data class Habit(
    val id: Long = 0,
    val name: String = "",
    val color: Long = Palette.DEFAULT,
    /** Daily reminder, minutes since midnight. */
    val reminderTime: Int? = null,
    val doneDays: List<Long> = emptyList(),
    val order: Int = 0,
)

@Serializable
data class JournalEntry(
    val id: Long = 0,
    val date: Long = 0,
    val text: String = "",
)

@Serializable
data class AppData(
    val mission: String = "",
    val roles: List<Role> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val journal: List<JournalEntry> = emptyList(),
    val seeded: Boolean = false,
)

enum class Quadrant(val important: Boolean, val urgent: Boolean) {
    I(true, true), II(true, false), III(false, true), IV(false, false)
}

val Task.quadrant: Quadrant
    get() = Quadrant.entries.first { it.important == important && it.urgent == urgent }

/** Moves the task to another day, shifting its reminder by the same number of days. */
fun Task.movedTo(newDate: Long?): Task {
    if (newDate == date) return this
    val reminder = reminderAt
    val shifted = if (reminder != null && date != null && newDate != null) {
        Instant.ofEpochMilli(reminder).atZone(ZoneId.systemDefault())
            .plusDays(newDate - date).toInstant().toEpochMilli()
    } else reminder
    return copy(date = newDate, reminderAt = shifted)
}

fun Habit.isDone(day: Long) = day in doneDays

fun Habit.streak(today: Long): Int {
    val days = doneDays.toHashSet()
    var day = if (today in days) today else today - 1
    var count = 0
    while (day in days) {
        count++
        day--
    }
    return count
}
