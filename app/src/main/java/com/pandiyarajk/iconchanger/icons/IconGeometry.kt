/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.icons

/**
 * Pure icon maths, kept apart from drawing so it runs in a plain JVM test.
 *
 * An adaptive icon is a 108dp canvas of which the launcher shows a mask-shaped window; only the
 * central 66dp is guaranteed visible. A plain icon is drawn as is, so it carries its own mask.
 */
object IconGeometry {
    /** Fraction of the canvas that is always visible in an adaptive icon (66dp of 108dp). */
    const val ADAPTIVE_SAFE_FRACTION = 66f / 108f

    /** Glyph side as a fraction of the canvas. Adaptive stays well inside the safe zone. */
    fun glyphFraction(adaptive: Boolean): Float = if (adaptive) 0.5f else 0.6f

    /** Corner radius as a fraction of the icon side. A radius of 0.5 makes a circle. */
    fun cornerFraction(shape: IconShape): Float = when (shape) {
        IconShape.CIRCLE -> 0.5f
        IconShape.ROUNDED -> 0.22f
        IconShape.SQUARE -> 0f
    }

    /** Largest offset, as a fraction of the icon side, that keeps a zoomed photo covering it. */
    fun maxOffset(zoom: Float): Float = (zoom.coerceAtLeast(1f) - 1f) / 2f

    /** Clamps [value] so a photo at [zoom] never shows an empty edge. */
    fun clampOffset(value: Float, zoom: Float): Float {
        val limit = maxOffset(zoom)
        return value.coerceIn(-limit, limit)
    }

    /** True when the glyph box fits inside the adaptive safe zone, centred. */
    fun glyphFitsSafeZone(adaptive: Boolean): Boolean =
        !adaptive || glyphFraction(true) <= ADAPTIVE_SAFE_FRACTION
}
