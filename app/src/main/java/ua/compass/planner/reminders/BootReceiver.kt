package ua.compass.planner.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ua.compass.planner.CompassApp

/** Alarms are cleared on reboot and time changes, so book them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as CompassApp
        app.scheduler.sync(app.repository.data.value)
    }
}
