package com.neverreader.app.list

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReadingTimeLabelTest {

    @Test
    fun `renders minutes`() {
        assertEquals("1 min read", readingTimeLabel(1))
        assertEquals("6 min read", readingTimeLabel(6))
    }

    @Test
    fun `drops a missing or zero reading time instead of rendering 0 min`() {
        assertNull(readingTimeLabel(null))
        assertNull(readingTimeLabel(0))
        assertNull(readingTimeLabel(-3))
    }

    @Test
    fun `caps absurd reading times`() {
        assertEquals("60+ min read", readingTimeLabel(MAX_READING_TIME_LABEL + 1))
    }
}
