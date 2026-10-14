/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewModelScope
import com.pandiyarajk.iconchanger.apps.AppInfo
import com.pandiyarajk.iconchanger.apps.AppRepository
import com.pandiyarajk.iconchanger.data.HistoryEntry
import com.pandiyarajk.iconchanger.data.HistoryStore
import com.pandiyarajk.iconchanger.icons.IconRenderer
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
import java.io.InputStream

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
    private val originalDir = File(application.filesDir, "originals")

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
                removed.forEach {
                    deletePhotos(it.packageName)
                    File(originalDir, "${it.packageName}.png").delete()
                }
            }
            _loading.value = false
        }
    }

    fun record(entry: HistoryEntry) {
        viewModelScope.launch { store.put(entry) }
    }

    /** Forgets a customisation (after a restore) and drops its picked photo. */
    fun forget(packageName: String) {
        viewModelScope.launch {
            store.remove(packageName)
            withContext(Dispatchers.IO) { deletePhotos(packageName) }
        }
    }

    /**
     * Saves the app's current icon the first time it is seen and returns the file path. Taken
     * before any change, so it is the real original; once saved it is never overwritten.
     */
    suspend fun ensureOriginal(app: AppInfo): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(originalDir, "${app.packageName}.png")
            if (!file.exists()) {
                originalDir.mkdirs()
                val bitmap = app.icon.toBitmap(IconRenderer.SHORTCUT_SIZE_PX, IconRenderer.SHORTCUT_SIZE_PX)
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
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
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@withContext null
            // Camera photos are stored sideways with an EXIF rotation; BitmapFactory ignores it.
            val degrees = resolver.openInputStream(uri)?.use { exifRotation(it) } ?: 0
            val bitmap = if (degrees == 0) decoded else {
                val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            }
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

    private fun exifRotation(stream: InputStream): Int = try {
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }

    private fun deletePhotos(packageName: String) {
        photoDir.listFiles { f -> f.name.startsWith("${packageName}_") }?.forEach { it.delete() }
    }

    private companion object {
        const val MAX_PHOTO_PX = 1024
    }
}
