package ua.compass.planner.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import ua.compass.planner.data.AppData
import java.time.LocalDate
import java.time.ZoneId

/** Keeps AlarmManager in step with the reminders stored in [AppData]. */
class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private var scheduled: Map<String, Long> = emptyMap()

    @Synchronized
    fun sync(data: AppData) {
        val now = System.currentTimeMillis()
        val wanted = HashMap<String, Long>()
        data.tasks.forEach { task ->
            val at = task.reminderAt
            if (!task.done && at != null && at > now) wanted["task:${task.id}"] = at
        }
        data.goals.forEach { goal ->
            val at = goal.reminderAt
            if (!goal.done && at != null && at > now) wanted["goal:${goal.id}"] = at
        }
        data.habits.forEach { habit ->
            habit.reminderTime?.let { wanted["habit:${habit.id}"] = nextDaily(it, now) }
        }
        (scheduled.keys - wanted.keys).forEach { cancel(it) }
        wanted.forEach { (key, at) -> if (scheduled[key] != at) schedule(key, at) }
        scheduled = wanted
    }

    private fun nextDaily(minutes: Int, now: Long): Long {
        var at = LocalDate.now().atTime(minutes / 60, minutes % 60).atZone(ZoneId.systemDefault())
        if (at.toInstant().toEpochMilli() <= now) at = at.plusDays(1)
        return at.toInstant().toEpochMilli()
    }

    private fun pendingIntent(key: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        key.hashCode(),
        Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION)
            .setData(Uri.parse("compass://reminder/$key"))
            .putExtra(ReminderReceiver.EXTRA_KEY, key),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun schedule(key: String, at: Long) {
        val intent = pendingIntent(key)
        try {
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
        } catch (e: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    private fun cancel(key: String) = alarms.cancel(pendingIntent(key))
}
