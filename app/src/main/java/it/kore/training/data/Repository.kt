package it.kore.training.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/** All data lives in one JSON file in the app's private storage: fully offline. */
class Repository(context: Context) {

    private val file = File(context.filesDir, "kore_data.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data

    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString(AppData.serializer(), file.readText()) else AppData()
    } catch (e: Exception) {
        AppData()
    }

    fun update(transform: (AppData) -> AppData) {
        synchronized(this) { _data.value = transform(_data.value) }
        val snapshot = _data.value
        scope.launch {
            mutex.withLock {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(json.encodeToString(AppData.serializer(), snapshot))
                tmp.renameTo(file)
            }
        }
    }

    fun exportJson(): String = json.encodeToString(AppData.serializer(), _data.value)

    fun importJson(text: String): Boolean = try {
        val d = json.decodeFromString(AppData.serializer(), text)
        update { d }
        true
    } catch (e: Exception) {
        false
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
}
