/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.pandiyarajk.iconchanger.ui.AppListScreen
import com.pandiyarajk.iconchanger.ui.IconEditorScreen
import com.pandiyarajk.iconchanger.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IconChangerTheme {
                Surface(color = MaterialTheme.colorScheme.background) { Root(viewModel) }
            }
        }
    }
}

@Composable
private fun Root(viewModel: MainViewModel) {
    val apps by viewModel.apps.collectAsState()
    val history by viewModel.history.collectAsState()
    val loading by viewModel.loading.collectAsState()
    // No navigation library: the selected package is the whole back stack.
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val app = apps.firstOrNull { it.packageName == selected }

    BackHandler(enabled = app != null) { selected = null }

    if (app == null) {
        AppListScreen(
            apps = apps,
            history = history,
            loading = loading,
            onRefresh = viewModel::refresh,
            onSelect = { selected = it.packageName },
        )
    } else {
        IconEditorScreen(
            app = app,
            initial = history[app.packageName],
            importPhoto = { uri -> viewModel.importPhoto(uri, app.packageName) },
            onApplied = viewModel::record,
            onBack = { selected = null },
        )
    }
}

@Composable
private fun IconChangerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
