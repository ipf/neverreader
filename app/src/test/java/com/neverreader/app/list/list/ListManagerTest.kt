package com.neverreader.app.list.list

import com.neverreader.BaseCoroutineTest
import com.neverreader.sdk.Pocket
import com.neverreader.sdk.api.generated.enums.ItemFilterKey
import com.neverreader.sdk.api.generated.enums.ItemSortKey
import com.neverreader.sdk2.api.legacy.PocketCache
import com.neverreader.util.prefs.Preferences
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListManagerTest : BaseCoroutineTest() {

    @SpyK private val pocket = mockk<Pocket>(relaxed = true)
    @SpyK private val prefs = mockk<Preferences>(relaxed = true)
    @SpyK private val pocketCache = mockk<PocketCache>(relaxed = true)

    private lateinit var subject: ListManager

    private val sortFilterState: SortFilterState
        get() = subject.sortFilterState.value

    @BeforeTest
    fun setup() {
        subject = ListManager(
            pocket = pocket,
            prefs = prefs,
            pocketCache = pocketCache
        )
    }

    @Test
    fun `set tag`() {
        subject.setTag("test")
        assertEquals(
            sortFilterState.tag,
            "test"
        )
        assertEquals(
            sortFilterState.filters,
            listOf(ItemFilterKey.TAG)
        )
    }

    @Test
    fun `updating sort`() {
        subject.updateCurrentSort(ItemSortKey.OLDEST)
        assertEquals(
            sortFilterState.sort,
            ItemSortKey.OLDEST
        )
    }

    @Test
    fun `filter toggle`() {
        subject.onFilterToggled(ItemFilterKey.FAVORITE)
        assertEquals(
            sortFilterState.tag,
            null
        )
        assertEquals(
            sortFilterState.filters,
            listOf(ItemFilterKey.FAVORITE)
        )

        subject.onFilterToggled(ItemFilterKey.HIGHLIGHTED)
        assertEquals(
            sortFilterState.filters,
            listOf(ItemFilterKey.HIGHLIGHTED)
        )

        subject.onFilterToggled(ItemFilterKey.HIGHLIGHTED)
        assertEquals(
            sortFilterState.filters,
            listOf()
        )
    }

    @Test
    fun `add filter`() {
        subject.addFilter(ItemFilterKey.FAVORITE)
        assertEquals(
            sortFilterState.tag,
            null
        )
        assertEquals(
            sortFilterState.filters,
            listOf(ItemFilterKey.FAVORITE)
        )
    }

    @Test
    fun `clear filters`() {
        subject.addFilter(ItemFilterKey.FAVORITE)
        subject.clearFilters()
        assertEquals(
            sortFilterState.filters,
            listOf()
        )
    }

    @Test
    fun `status filter`() {
        subject.setStatusFilter(ListStatus.ARCHIVE)
        assertEquals(
            sortFilterState.listStatus,
            ListStatus.ARCHIVE
        )
        assertEquals(
            sortFilterState.filters,
            listOf()
        )
        assertEquals(
            sortFilterState.tag,
            null
        )
    }

    @Test
    fun `search text`() {
        subject.setSearchText("test")

        assertEquals(
            expected = listOf(),
            actual = sortFilterState.filters,
        )

        assertEquals(
            expected = "test",
            actual = sortFilterState.search
        )

        assertEquals(
            expected = null,
            actual = sortFilterState.tag,
        )
    }
}