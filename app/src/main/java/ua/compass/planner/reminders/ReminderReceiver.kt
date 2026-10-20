package ua.compass.planner.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ua.compass.planner.CompassApp
import ua.compass.planner.R
import ua.compass.planner.data.Settings
import ua.compass.planner.data.isDone
import ua.compass.planner.util.Dates

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(EXTRA_KEY) ?: return
        val app = context.applicationContext as CompassApp
        val data = app.repository.data.value
        val localized = Settings.wrapLocale(context)
        val id = key.substringAfter(':').toLongOrNull() ?: return
        val content: Pair<String, String>? = when (key.substringBefore(':')) {
            "task" -> data.tasks.firstOrNull { it.id == id && !it.done }?.let {
                it.title to it.notes.ifBlank { localized.getString(R.string.notif_task) }
            }
            "goal" -> data.goals.firstOrNull { it.id == id && !it.done }?.let {
                it.title to localized.getString(R.string.notif_goal)
            }
            "habit" -> data.habits.firstOrNull { it.id == id && !it.isDone(Dates.todayEpoch()) }?.let {
                it.name to localized.getString(R.string.notif_habit)
            }
            else -> null
        }
        content?.let { (title, text) -> Notifications.show(localized, key.hashCode(), title, text) }
        // Daily habit reminders need their next occurrence booked.
        app.scheduler.sync(data)
    }

    companion object {
        const val ACTION = "ua.compass.planner.REMINDER"
        const val EXTRA_KEY = "key"
    }
}
