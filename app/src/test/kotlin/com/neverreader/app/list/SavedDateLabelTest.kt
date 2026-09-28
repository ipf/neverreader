package com.neverreader.app.list

import java.time.ZoneId
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The save date shown at the bottom-left of a row.
 *
 * It is the date the item was saved, not when the article was published -
 * neither backend records a publication date.
 */
class SavedDateLabelTest {

    private val london = ZoneId.of("Europe/London")
    private val uk = Locale.UK

    private fun label(iso: String) =
        savedDateLabel(java.time.Instant.parse(iso).toEpochMilli(), london, uk)

    @Test
    fun `renders the date it was saved`() {
        assertEquals("27 Sept 2026", label("2026-09-27T10:15:00Z"))
    }

    @Test
    fun `follows the given locale rather than hardcoding an order`() {
        val german = Locale.GERMANY
        val value = java.time.Instant.parse("2026-09-27T10:15:00Z").toEpochMilli()

        val ukText = savedDateLabel(value, london, uk)
        val deText = savedDateLabel(value, london, german)

        assertEquals(ukText, "27 Sept 2026")
        assertEquals(deText, "27.09.2026")
    }

    @Test
    fun `an unset timestamp renders nothing rather than 1970`() {
        assertNull(savedDateLabel(0L, london, uk))
        assertNull(savedDateLabel(-1L, london, uk))
    }

    @Test
    fun `the same instant on either side of midnight differs in the local zone`() {
        // 23:30 UTC is already the next day in London, so the label must follow
        // the device's zone rather than UTC.
        assertEquals("28 Sept 2026", label("2026-09-27T23:30:00Z"))
        assertEquals("27 Sept 2026", label("2026-09-27T09:30:00Z"))
    }
}
