/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.icons

import kotlinx.serialization.Serializable

/** Outline applied to a plain (non-adaptive) icon. Adaptive icons are masked by the launcher. */
enum class IconShape { CIRCLE, ROUNDED, SQUARE }

/** Where the icon artwork comes from. */
enum class IconSource { LIBRARY, PHOTO }

/**
 * Everything needed to redraw an icon, small enough to store in history.
 *
 * Colours are ARGB ints widened to Long so kotlinx.serialization keeps them unsigned-safe.
 * [photoPath] is a file inside the app's own storage: the Photo Picker grants only a temporary
 * read, so the picked image is copied at pick time. [offsetX] and [offsetY] are fractions of
 * the icon side, clamped by [IconGeometry.clampOffset].
 */
@Serializable
data class IconSpec(
    val source: IconSource = IconSource.LIBRARY,
    val glyphId: String = "star",
    val background: Long = 0xFF3949ABL,
    val foreground: Long = 0xFFFFFFFFL,
    val shape: IconShape = IconShape.ROUNDED,
    val adaptive: Boolean = false,
    val photoPath: String? = null,
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)
