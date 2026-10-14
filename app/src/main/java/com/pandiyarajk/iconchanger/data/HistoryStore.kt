/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pandiyarajk.iconchanger.icons.IconSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One applied change: which app, the label it was given and how the icon is drawn. */
@Serializable
data class HistoryEntry(
    val packageName: String,
    val label: String,
    val spec: IconSpec,
)

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore("history")

/** Applied changes, keyed by package, so the editor reopens with the last choice. */
class HistoryStore(private val context: Context) {
    private val key = stringPreferencesKey("entries")
    private val json = Json { ignoreUnknownKeys = true }

    val entries: Flow<Map<String, HistoryEntry>> = context.historyDataStore.data.map { prefs ->
        decode(prefs[key])
    }

    suspend fun put(entry: HistoryEntry) {
        context.historyDataStore.edit { prefs ->
            val current = decode(prefs[key]).toMutableMap()
            current[entry.packageName] = entry
            prefs[key] = encode(current)
        }
    }

    /** Drops entries whose app is no longer installed and returns their package names. */
    suspend fun prune(installed: Set<String>): List<HistoryEntry> {
        var removed: List<HistoryEntry> = emptyList()
        context.historyDataStore.edit { prefs ->
            val current = decode(prefs[key])
            val (keep, drop) = current.values.partition { it.packageName in installed }
            removed = drop
            if (drop.isNotEmpty()) prefs[key] = encode(keep.associateBy { it.packageName })
        }
        return removed
    }

    private fun decode(raw: String?): Map<String, HistoryEntry> {
        if (raw.isNullOrEmpty()) return emptyMap()
        // A corrupt or older-format value must not take the app list down with it.
        return try {
            json.decodeFromString<List<HistoryEntry>>(raw).associateBy { it.packageName }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun encode(map: Map<String, HistoryEntry>): String =
        json.encodeToString(map.values.toList())
}
