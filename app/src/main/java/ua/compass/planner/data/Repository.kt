package ua.compass.planner.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ua.compass.planner.R
import java.io.File
import java.util.concurrent.atomic.AtomicLong

/** Keeps all app data in memory and persists it as a JSON file in private storage. */
class Repository(private val context: Context) {
    private val file = File(context.filesDir, "data.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val ids = AtomicLong(System.currentTimeMillis())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val saveRequests = Channel<Unit>(Channel.CONFLATED)

    private val _data = MutableStateFlow(seed(load()))
    val data: StateFlow<AppData> = _data.asStateFlow()

    init {
        bumpIds(_data.value)
        scope.launch { for (request in saveRequests) save(_data.value) }
        saveRequests.trySend(Unit)
    }

    fun newId(): Long = ids.incrementAndGet()

    fun update(transform: (AppData) -> AppData) {
        _data.update(transform)
        saveRequests.trySend(Unit)
    }

    fun exportJson(): String =
        Json { prettyPrint = true; encodeDefaults = true }.encodeToString(AppData.serializer(), _data.value)

    fun importJson(text: String): Boolean = try {
        val imported = json.decodeFromString(AppData.serializer(), text).copy(seeded = true)
        bumpIds(imported)
        _data.value = imported
        saveRequests.trySend(Unit)
        true
    } catch (e: Exception) {
        false
    }

    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    } catch (e: Exception) {
        file.copyTo(File(context.filesDir, "data-unreadable-${System.currentTimeMillis()}.json"), overwrite = true)
        AppData()
    }

    @Synchronized
    private fun save(data: AppData) {
        val tmp = File(file.path + ".tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), data))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private fun seed(data: AppData): AppData {
        if (data.seeded) return data
        val roles = data.roles.ifEmpty {
            listOf(
                Role(
                    id = 1,
                    name = context.getString(R.string.seed_role_saw),
                    description = context.getString(R.string.seed_role_saw_desc),
                    color = Palette.colors[3],
                )
            )
        }
        return data.copy(roles = roles, seeded = true)
    }

    private fun bumpIds(data: AppData) {
        val max = (data.roles.map { it.id } + data.goals.map { it.id } + data.tasks.map { it.id } +
            data.habits.map { it.id } + data.journal.map { it.id }).maxOrNull() ?: 0
        ids.updateAndGet { maxOf(it, max) }
    }
}
