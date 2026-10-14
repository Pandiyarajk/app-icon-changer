/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.shortcut

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/** Stable shortcut ids: re-applying to the same app must update, not duplicate. */
object ShortcutIds {
    fun forTarget(packageName: String): String = "icon_$packageName"
}

enum class ApplyOutcome {
    /** The launcher was asked to pin a new shortcut and will show its own confirmation. */
    PIN_REQUESTED,

    /** A shortcut for this app was already pinned, so it was updated in place. */
    UPDATED,

    /** The default launcher cannot pin shortcuts. */
    NOT_SUPPORTED,
}

/**
 * Android cannot change another app's real launcher icon, so the "changed icon" is a pinned
 * home-screen shortcut that launches the target with our icon and label.
 */
object ShortcutCreator {

    fun apply(
        context: Context,
        packageName: String,
        className: String,
        label: String,
        icon: Bitmap,
        adaptive: Boolean,
    ): ApplyOutcome {
        val id = ShortcutIds.forTarget(packageName)
        val launch = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(packageName, className))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val iconCompat = if (adaptive) {
            IconCompat.createWithAdaptiveBitmap(icon)
        } else {
            IconCompat.createWithBitmap(icon)
        }
        val info = ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(iconCompat)
            .setIntent(launch)
            .build()

        val alreadyPinned = ShortcutManagerCompat
            .getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
            .any { it.id == id }
        return when {
            alreadyPinned -> {
                ShortcutManagerCompat.updateShortcuts(context, listOf(info))
                ApplyOutcome.UPDATED
            }
            !ShortcutManagerCompat.isRequestPinShortcutSupported(context) -> ApplyOutcome.NOT_SUPPORTED
            else -> {
                ShortcutManagerCompat.requestPinShortcut(context, info, null)
                ApplyOutcome.PIN_REQUESTED
            }
        }
    }

    /**
     * Puts an already pinned shortcut back to the original icon and label. Returns false when
     * none is pinned, in which case there is nothing on the home screen to restore.
     */
    fun restore(
        context: Context,
        packageName: String,
        className: String,
        label: String,
        original: Bitmap,
    ): Boolean {
        val id = ShortcutIds.forTarget(packageName)
        val pinned = ShortcutManagerCompat
            .getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
            .any { it.id == id }
        if (!pinned) return false
        val launch = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(packageName, className))
        val info = ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithBitmap(original))
            .setIntent(launch)
            .build()
        ShortcutManagerCompat.updateShortcuts(context, listOf(info))
        return true
    }

    /** Disables the pinned shortcuts of apps that are gone, so they stop launching nothing. */
    fun disable(context: Context, packageNames: Collection<String>) {
        if (packageNames.isEmpty()) return
        ShortcutManagerCompat.disableShortcuts(
            context,
            packageNames.map(ShortcutIds::forTarget),
            "App uninstalled",
        )
    }
}
