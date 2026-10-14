/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger.icons

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

/**
 * Draws an [IconSpec] to a bitmap. The same drawing serves the live preview and the shortcut,
 * so what the editor shows is what the launcher receives.
 */
object IconRenderer {
    /** 108dp at xxxhdpi, so an adaptive icon stays sharp on the densest screens. */
    const val SHORTCUT_SIZE_PX = 432

    /**
     * Renders [spec] at [sizePx]. [glyph] is the library artwork (null for a photo icon) and
     * [photo] the decoded picture (null for a library icon).
     */
    fun render(
        spec: IconSpec,
        glyph: Painter?,
        photo: ImageBitmap?,
        sizePx: Int = SHORTCUT_SIZE_PX,
    ): ImageBitmap {
        val bitmap = ImageBitmap(sizePx, sizePx, ImageBitmapConfig.Argb8888)
        val size = Size(sizePx.toFloat(), sizePx.toFloat())
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap), size) {
            drawIcon(spec, glyph, photo)
        }
        return bitmap
    }

    private fun DrawScope.drawIcon(spec: IconSpec, glyph: Painter?, photo: ImageBitmap?) {
        val side = size.minDimension
        // The launcher masks an adaptive icon itself, so ours must be full-bleed.
        val shape = if (spec.adaptive) IconShape.SQUARE else spec.shape
        val radius = IconGeometry.cornerFraction(shape) * side
        val outline = Path().apply {
            addRoundRect(RoundRect(0f, 0f, side, side, CornerRadius(radius, radius)))
        }
        clipPath(outline) {
            if (spec.source == IconSource.PHOTO && photo != null) {
                drawPhoto(spec, photo, side)
            } else {
                drawRect(Color(spec.background.toInt()))
                if (glyph != null) drawGlyph(spec, glyph, side)
            }
        }
    }

    private fun DrawScope.drawGlyph(spec: IconSpec, glyph: Painter, side: Float) {
        val box = side * IconGeometry.glyphFraction(spec.adaptive)
        translate((side - box) / 2f, (side - box) / 2f) {
            with(glyph) {
                draw(Size(box, box), colorFilter = ColorFilter.tint(Color(spec.foreground.toInt())))
            }
        }
    }

    private fun DrawScope.drawPhoto(spec: IconSpec, photo: ImageBitmap, side: Float) {
        // Centre-crop to a square first, then apply the user's zoom and pan.
        val square = min(photo.width, photo.height)
        val srcOffset = IntOffset((photo.width - square) / 2, (photo.height - square) / 2)
        val zoom = spec.zoom.coerceAtLeast(1f)
        val dx = IconGeometry.clampOffset(spec.offsetX, zoom) * side
        val dy = IconGeometry.clampOffset(spec.offsetY, zoom) * side
        withTransform({
            translate(dx, dy)
            scale(zoom, zoom, Offset(side / 2f, side / 2f))
        }) {
            drawImage(
                image = photo,
                srcOffset = srcOffset,
                srcSize = IntSize(square, square),
                dstSize = IntSize(side.toInt(), side.toInt()),
            )
        }
    }
}
