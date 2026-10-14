/*
 * Author: Pandiyaraj Karuppasamy
 * Date: Oct-14-2026
 */
package com.pandiyarajk.iconchanger

import com.pandiyarajk.iconchanger.data.HistoryEntry
import com.pandiyarajk.iconchanger.icons.IconCatalog
import com.pandiyarajk.iconchanger.icons.IconCategory
import com.pandiyarajk.iconchanger.icons.IconGeometry
import com.pandiyarajk.iconchanger.icons.IconShape
import com.pandiyarajk.iconchanger.icons.IconSource
import com.pandiyarajk.iconchanger.icons.IconSpec
import com.pandiyarajk.iconchanger.shortcut.ShortcutIds
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconLogicTest {

    @Test
    fun catalogIdsAreUnique() {
        val ids = IconCatalog.all.map { it.id }
        assertEquals(ids.distinct().size, ids.size)
    }

    @Test
    fun everyCategoryHasIcons() {
        IconCategory.entries.forEach { c ->
            assertTrue("${c.label} is empty", IconCatalog.all.any { it.category == c })
        }
    }

    @Test
    fun searchMatchesNameAndRespectsCategory() {
        assertTrue(IconCatalog.search("camera", null).any { it.id == "photo_camera" })
        assertTrue(IconCatalog.search("camera", IconCategory.FINANCE).isEmpty())
        assertEquals(IconCatalog.all.size, IconCatalog.search("  ", null).size)
    }

    @Test
    fun findReturnsNullForUnknownId() {
        assertNotNull(IconCatalog.find("star"))
        assertNull(IconCatalog.find("no_such_icon"))
    }

    @Test
    fun shortcutIdIsStablePerPackage() {
        assertEquals("icon_com.example.app", ShortcutIds.forTarget("com.example.app"))
        assertEquals(ShortcutIds.forTarget("a.b"), ShortcutIds.forTarget("a.b"))
    }

    @Test
    fun circleHasHalfSideRadiusAndSquareNone() {
        assertEquals(0.5f, IconGeometry.cornerFraction(IconShape.CIRCLE), 0f)
        assertEquals(0f, IconGeometry.cornerFraction(IconShape.SQUARE), 0f)
    }

    @Test
    fun adaptiveGlyphStaysInsideSafeZone() {
        assertTrue(IconGeometry.glyphFraction(true) <= IconGeometry.ADAPTIVE_SAFE_FRACTION)
        assertTrue(IconGeometry.glyphFitsSafeZone(true))
    }

    @Test
    fun offsetIsClampedSoPhotoAlwaysCoversIcon() {
        assertEquals(0f, IconGeometry.clampOffset(0.9f, 1f), 0f)
        assertEquals(0.5f, IconGeometry.clampOffset(0.9f, 2f), 0f)
        assertEquals(-0.5f, IconGeometry.clampOffset(-0.9f, 2f), 0f)
        assertEquals(0.1f, IconGeometry.clampOffset(0.1f, 2f), 0f)
    }

    @Test
    fun specSurvivesJsonRoundTrip() {
        val spec = IconSpec(glyphId = "home", background = 0xFFE53935L, adaptive = true, zoom = 2.5f)
        val json = Json.encodeToString(spec)
        assertEquals(spec, Json.decodeFromString<IconSpec>(json))
    }

    @Test
    fun historyKeepsOriginalLabelAndReadsOlderEntries() {
        val entry = HistoryEntry("a.b", "Mine", IconSpec(source = IconSource.ORIGINAL), originalLabel = "Real")
        val back = Json.decodeFromString<HistoryEntry>(Json.encodeToString(entry))
        assertEquals("Real", back.originalLabel)
        assertEquals(IconSource.ORIGINAL, back.spec.source)
        // An entry saved before originalLabel existed has no such key and must still load.
        val old = Json { ignoreUnknownKeys = true }
            .decodeFromString<HistoryEntry>("""{"packageName":"a.b","label":"x","spec":{}}""")
        assertEquals("", old.originalLabel)
    }
}
