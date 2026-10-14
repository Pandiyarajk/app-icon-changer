/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pandiyarajk.iconchanger.apps.AppInfo
import com.pandiyarajk.iconchanger.apps.AppRepository
import com.pandiyarajk.iconchanger.data.HistoryEntry
import com.pandiyarajk.iconchanger.data.HistoryStore
import com.pandiyarajk.iconchanger.shortcut.ShortcutCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Holds the app list and the history; manual DI, no Hilt. */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val store = HistoryStore(application)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val history: StateFlow<Map<String, HistoryEntry>> = store.entries
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val photoDir = File(application.filesDir, "photos")

    init {
        refresh()
    }

    /** Reloads the launchable apps and cleans up history for apps that were uninstalled. */
    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            val list = withContext(Dispatchers.Default) { repository.loadLaunchable() }
            _apps.value = list
            val removed = store.prune(list.map { it.packageName }.toSet())
            ShortcutCreator.disable(getApplication(), removed.map { it.packageName })
            withContext(Dispatchers.IO) {
                removed.forEach { deletePhotos(it.packageName) }
            }
            _loading.value = false
        }
    }

    fun record(entry: HistoryEntry) {
        viewModelScope.launch { store.put(entry) }
    }

    /**
     * Copies a picked image into app storage, downsampled to at most [MAX_PHOTO_PX] on its long
     * side, and returns the file path. The Photo Picker's URI grant is only temporary.
     */
    suspend fun importPhoto(uri: Uri, packageName: String): String? = withContext(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PHOTO_PX) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext null
            photoDir.mkdirs()
            deletePhotos(packageName)
            // A fresh name each time, so the editor's decode cache never serves a stale picture.
            val file = File(photoDir, "${packageName}_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun deletePhotos(packageName: String) {
        photoDir.listFiles { f -> f.name.startsWith("${packageName}_") }?.forEach { it.delete() }
    }

    private companion object {
        const val MAX_PHOTO_PX = 1024
    }
}
