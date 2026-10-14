/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.apps

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

/** One launchable app. [icon] is the app's current launcher icon, for display only. */
data class AppInfo(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: Drawable,
)

/** Lists the apps that have a launcher entry, using the manifest `<queries>` visibility. */
class AppRepository(private val context: Context) {

    /** Blocking: call off the main thread. Own app excluded, sorted by label. */
    fun loadLaunchable(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolved ->
                AppInfo(
                    label = resolved.loadLabel(pm).toString(),
                    packageName = resolved.activityInfo.packageName,
                    className = resolved.activityInfo.name,
                    icon = resolved.loadIcon(pm),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
