package app.abypass.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("abypass", Context.MODE_PRIVATE)

    fun settings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs.getBoolean("dynamicColor", true),
            desktopMode = prefs.getBoolean("desktop", false),
            favorites = prefs.getStringSet("favorites", defaults.favorites)!!.toSet(),
            adapters = prefs.getStringSet("adapters", defaults.adapters)!!.toSet(),
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit().putString("theme", settings.theme.name)
            .putBoolean("dynamicColor", settings.dynamicColor)
            .putBoolean("desktop", settings.desktopMode)
            .putStringSet("favorites", settings.favorites)
            .putStringSet("adapters", settings.adapters).apply()
    }

    fun records(): Map<String, ServiceRecord> = Services.all.associate { service ->
        service.id to ServiceRecord(
            status = runCatching { CheckStatus.valueOf(prefs.getString("status_${service.id}", "NOT_CHECKED")!!) }.getOrDefault(CheckStatus.NOT_CHECKED),
            checkedAt = prefs.getLong("checked_${service.id}", 0),
            adapterChanges = prefs.getInt("changes_${service.id}", 0),
        )
    }

    fun saveRecord(id: String, record: ServiceRecord) {
        prefs.edit().putString("status_$id", record.status.name)
            .putLong("checked_$id", record.checkedAt)
            .putInt("changes_$id", record.adapterChanges).apply()
    }

    fun events(): List<DiagnosticEvent> = runCatching {
        val array = JSONArray(prefs.getString("events", "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            DiagnosticEvent(item.getLong("time"), item.getString("service"), item.getString("message"))
        }
    }.getOrDefault(emptyList())

    fun saveEvents(events: List<DiagnosticEvent>) {
        val array = JSONArray()
        events.takeLast(150).forEach {
            array.put(JSONObject().put("time", it.time).put("service", it.serviceId).put("message", it.message))
        }
        prefs.edit().putString("events", array.toString()).apply()
    }

    fun resetRecords() {
        prefs.edit().apply {
            Services.all.forEach { remove("status_${it.id}"); remove("checked_${it.id}"); remove("changes_${it.id}") }
        }.apply()
    }
}
