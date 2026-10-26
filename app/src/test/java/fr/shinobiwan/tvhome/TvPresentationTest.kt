package fr.shinobiwan.tvhome

import fr.shinobiwan.tvhome.ui.*
import org.junit.Assert.*
import org.junit.Test

class TvPresentationTest {
    @Test fun lastAppHasAFullPageSlot() {
        assertEquals(0, appPage(4, 12))
        assertEquals(1, appPage(5, 12))
        assertEquals(2, appPage(11, 12))
        assertEquals(10..11, pageRange(11, 12))
    }
    @Test fun removedAndEmptyAppsNeverProduceAnInvalidSelection() {
        assertEquals(0, boundedAppIndex(9, 0))
        assertEquals(2, boundedAppIndex(9, 3))
        assertTrue(pageRange(0, 0).isEmpty())
    }
    @Test fun arrowDoesNotWrapAtTheEdges() {
        assertEquals(0, moveApp(0, -1, 9))
        assertEquals(8, moveApp(8, 1, 9))
        assertEquals(5, moveApp(4, 1, 9))
    }
    @Test fun clockIsConsolidatedWithoutChangingStoredSelection() {
        val stored=listOf("codex", "media", "clock")
        assertEquals(listOf("codex","media"), homeWidgetIds(stored))
        assertEquals(3, stored.size)
    }
}
