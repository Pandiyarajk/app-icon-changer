/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pandiyarajk.iconchanger.apps.AppInfo
import com.pandiyarajk.iconchanger.data.HistoryEntry
import com.pandiyarajk.iconchanger.icons.IconCatalog
import com.pandiyarajk.iconchanger.icons.IconCategory
import com.pandiyarajk.iconchanger.icons.IconGeometry
import com.pandiyarajk.iconchanger.icons.IconRenderer
import com.pandiyarajk.iconchanger.icons.IconShape
import com.pandiyarajk.iconchanger.icons.IconSource
import com.pandiyarajk.iconchanger.icons.IconSpec
import com.pandiyarajk.iconchanger.shortcut.ApplyOutcome
import com.pandiyarajk.iconchanger.shortcut.ShortcutCreator
import kotlinx.coroutines.launch

private const val PREVIEW_PX = 288
private const val MAX_ZOOM = 5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconEditorScreen(
    app: AppInfo,
    initial: HistoryEntry?,
    importPhoto: suspend (Uri) -> String?,
    loadOriginal: suspend () -> String?,
    onApplied: (HistoryEntry) -> Unit,
    onRestored: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var label by rememberSaveable { mutableStateOf(initial?.label ?: app.label) }
    var spec by remember { mutableStateOf(initial?.spec ?: IconSpec()) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by remember { mutableStateOf<IconCategory?>(null) }
    var hex by rememberSaveable { mutableStateOf("") }

    val glyphVector = (IconCatalog.find(spec.glyphId) ?: IconCatalog.all.first()).vector
    val glyphPainter = rememberVectorPainter(glyphVector)
    val photo = remember(spec.photoPath) {
        spec.photoPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }
    // Saved the first time the app is opened here, before any change, so it is the real original.
    var originalPath by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(app.packageName) { originalPath = loadOriginal() }
    val original = remember(originalPath) {
        originalPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }
    val artwork = if (spec.source == IconSource.ORIGINAL) original else photo
    val preview = remember(spec, artwork, glyphPainter) {
        IconRenderer.render(spec, glyphPainter, artwork, PREVIEW_PX)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val path = importPhoto(uri)
            if (path == null) {
                Toast.makeText(context, "Could not read that image", Toast.LENGTH_SHORT).show()
            } else {
                spec = spec.copy(
                    source = IconSource.PHOTO, photoPath = path, zoom = 1f, offsetX = 0f, offsetY = 0f,
                )
            }
        }
    }

    val icons = remember(query, category) { IconCatalog.search(query, category) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app.label, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            bitmap = preview,
                            contentDescription = "Icon preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(144.dp)
                                .pointerInput(spec.source) {
                                    if (spec.source != IconSource.PHOTO) return@pointerInput
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val z = (spec.zoom * zoom).coerceIn(1f, MAX_ZOOM)
                                        spec = spec.copy(
                                            zoom = z,
                                            offsetX = IconGeometry.clampOffset(spec.offsetX + pan.x / size.width, z),
                                            offsetY = IconGeometry.clampOffset(spec.offsetY + pan.y / size.height, z),
                                        )
                                    }
                                },
                        )
                    }
                    if (spec.source == IconSource.PHOTO) {
                        Text(
                            "Drag and pinch the preview to crop.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Shortcut name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = spec.source == IconSource.LIBRARY,
                            onClick = { spec = spec.copy(source = IconSource.LIBRARY) },
                            label = { Text("Library") },
                        )
                        FilterChip(
                            selected = spec.source == IconSource.PHOTO,
                            onClick = {
                                if (spec.photoPath != null) {
                                    spec = spec.copy(source = IconSource.PHOTO)
                                } else {
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            },
                            label = { Text("My photo") },
                        )
                        FilterChip(
                            selected = spec.source == IconSource.ORIGINAL,
                            onClick = { spec = spec.copy(source = IconSource.ORIGINAL) },
                            enabled = original != null,
                            label = { Text("Original") },
                        )
                    }
                    if (spec.source == IconSource.PHOTO) {
                        OutlinedButton(onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text("Choose another photo") }
                    } else if (spec.source == IconSource.LIBRARY) {
                        LibraryControls(
                            spec = spec,
                            onSpec = { spec = it },
                            hex = hex,
                            onHex = { hex = it },
                        )
                    }
                    if (spec.source != IconSource.ORIGINAL) ShapeControls(spec) { spec = it }
                    if (spec.source == IconSource.LIBRARY) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search icons") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = category == null,
                                    onClick = { category = null },
                                    label = { Text("All") },
                                )
                            }
                            items(IconCategory.entries.toList()) { c ->
                                FilterChip(
                                    selected = category == c,
                                    onClick = { category = c },
                                    label = { Text(c.label) },
                                )
                            }
                        }
                    }
                }
            }
            if (spec.source == IconSource.LIBRARY) {
                items(icons, key = { it.id }) { icon ->
                    val selected = icon.id == spec.glyphId
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .clickable { spec = spec.copy(glyphId = icon.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon.vector, icon.name, Modifier.size(28.dp))
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val bitmap = IconRenderer.render(spec, glyphPainter, artwork).asAndroidBitmap()
                        val name = label.trim().ifEmpty { app.label }
                        val outcome = ShortcutCreator.apply(
                            context, app.packageName, app.className, name, bitmap,
                            spec.adaptive && spec.source != IconSource.ORIGINAL,
                        )
                        // The first label ever recorded is the app's own, kept for restore.
                        onApplied(
                            HistoryEntry(
                                app.packageName, name, spec,
                                originalLabel = initial?.originalLabel?.ifEmpty { null } ?: app.label,
                            ),
                        )
                        val message = when (outcome) {
                            ApplyOutcome.PIN_REQUESTED -> "Confirm adding the shortcut to your home screen"
                            ApplyOutcome.UPDATED -> "Shortcut updated"
                            ApplyOutcome.NOT_SUPPORTED -> "Your launcher cannot pin shortcuts"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    },
                    enabled = when (spec.source) {
                        IconSource.LIBRARY -> true
                        IconSource.PHOTO -> photo != null
                        IconSource.ORIGINAL -> original != null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add to home screen") }
                if (initial != null && original != null) {
                    OutlinedButton(
                        onClick = {
                            val restoredLabel = initial.originalLabel.ifEmpty { app.label }
                            val updated = ShortcutCreator.restore(
                                context, app.packageName, app.className, restoredLabel,
                                original.asAndroidBitmap(),
                            )
                            onRestored()
                            val message = if (updated) {
                                "Restored. Remove the shortcut from your home screen to drop it, " +
                                    "the app drawer icon was never changed."
                            } else {
                                "Customisation cleared. No shortcut was on your home screen."
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Restore original") }
                }
                }
            }
        }
    }
}

@Composable
private fun LibraryControls(spec: IconSpec, onSpec: (IconSpec) -> Unit, hex: String, onHex: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Background", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(IconCatalog.palette) { color ->
                val colorLong = color.toArgb().toLong() and 0xFFFFFFFFL
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            BorderStroke(
                                if (spec.background == colorLong) 3.dp else 1.dp,
                                if (spec.background == colorLong) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outline,
                            ),
                            CircleShape,
                        )
                        .clickable { onSpec(spec.copy(background = colorLong)) },
                )
            }
        }
        OutlinedTextField(
            value = hex,
            onValueChange = { text ->
                val clean = text.removePrefix("#").take(6).filter { it.isLetterOrDigit() }
                onHex(clean)
                if (clean.length == 6) {
                    clean.toLongOrNull(16)?.let { onSpec(spec.copy(background = 0xFF000000L or it)) }
                }
            },
            label = { Text("Custom colour (RRGGBB)") },
            prefix = { Text("#") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Icon colour", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = spec.foreground == 0xFFFFFFFFL,
                onClick = { onSpec(spec.copy(foreground = 0xFFFFFFFFL)) },
                label = { Text("White") },
            )
            FilterChip(
                selected = spec.foreground == 0xFF000000L,
                onClick = { onSpec(spec.copy(foreground = 0xFF000000L)) },
                label = { Text("Black") },
            )
        }
    }
}

@Composable
private fun ShapeControls(spec: IconSpec, onSpec: (IconSpec) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(checked = spec.adaptive, onCheckedChange = { onSpec(spec.copy(adaptive = it)) })
            Text("Let the launcher shape it (adaptive)", style = MaterialTheme.typography.bodyMedium)
        }
        if (!spec.adaptive) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconShape.entries.forEach { shape ->
                    FilterChip(
                        selected = spec.shape == shape,
                        onClick = { onSpec(spec.copy(shape = shape)) },
                        label = { Text(shape.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
        }
    }
}
