/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pandiyarajk.iconchanger.apps.AppInfo
import com.pandiyarajk.iconchanger.data.HistoryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    apps: List<AppInfo>,
    history: Map<String, HistoryEntry>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onSelect: (AppInfo) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(apps, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Icon Changer") },
                actions = {
                    IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Reload apps") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Search apps") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (loading && apps.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(shown, key = { it.packageName }) { app ->
                        AppRow(app, custom = app.packageName in history, onClick = { onSelect(app) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, custom: Boolean, onClick: () -> Unit) {
    val image = remember(app.packageName) { app.icon.toBitmap(96, 96).asImageBitmap() }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(image, contentDescription = null, modifier = Modifier.size(44.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (custom) {
            Text("Custom", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
