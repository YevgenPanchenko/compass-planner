package ua.compass.planner

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ua.compass.planner.data.Repository
import ua.compass.planner.data.Settings
import ua.compass.planner.reminders.Notifications
import ua.compass.planner.reminders.ReminderScheduler

class CompassApp : Application() {
    lateinit var settings: Settings
        private set
    lateinit var repository: Repository
        private set
    lateinit var scheduler: ReminderScheduler
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        settings = Settings(this)
        repository = Repository(this)
        scheduler = ReminderScheduler(this)
        Notifications.createChannel(this)
        scope.launch { repository.data.collect { scheduler.sync(it) } }
    }
}
