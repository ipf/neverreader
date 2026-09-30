package com.neverreader.util.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the Flow rewrite of the preference change notifications. The Rx version
 * had no tests at all, which is how a maintenance-mode library ended up being
 * the public type of this package.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PrefStoreTest {

    private lateinit var store: AndroidPrefStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = AndroidPrefStore(context.getSharedPreferences("prefs-test", Context.MODE_PRIVATE))
        store.clear()
    }

    @Test
    fun `string round trips`() {
        store.set("k", "v")
        assertEquals("v", store.getString("k"))
        assertTrue(store.contains("k"))
        assertNull(store.getString("missing"))
    }

    @Test
    fun `remove clears a value`() {
        store.set("k", "v")
        store.remove("k")
        assertFalse(store.contains("k"))
    }

    @Test
    fun `clear removes everything`() {
        store.set("a", "1")
        store.clear()
        assertFalse(store.contains("a"))
    }

    @Test
    fun `changes emits the key that changed`() = runTest {
        val seen = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.changes().collect { seen.add(it) }
        }
        runCurrent()

        store.set("a", "1")
        store.set("b", "2")
        runCurrent()

        assertEquals(listOf("a", "b"), seen)
    }

    @Test
    fun `changes ignores unrelated keys`() = runTest {
        var count = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.intChanges("watched").collect { count += 1 }
        }
        runCurrent()

        store.set("other", 1)
        runCurrent()
        assertEquals(0, count)

        store.set("watched", 7)
        runCurrent()
        assertEquals(1, count)
    }

    @Test
    fun `typed changes emit the new value, not just the key`() = runTest {
        val seen = mutableListOf<Int?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.intChanges("n").collect { seen.add(it) }
        }
        runCurrent()

        store.set("n", 1)
        store.set("n", 2)
        runCurrent()

        assertEquals(listOf<Int?>(1, 2), seen)
    }

    @Test
    fun `boolean changes emit the new value`() = runTest {
        val seen = mutableListOf<Boolean?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            store.booleanChanges("b").collect { seen.add(it) }
        }
        runCurrent()

        store.set("b", true)
        runCurrent()

        assertEquals(listOf<Boolean?>(true), seen)
    }

    @Test
    fun `collection stops when the scope is cancelled`() = runTest {
        var count = 0
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            store.changes().collect { count += 1 }
        }
        runCurrent()

        store.set("a", "1")
        runCurrent()
        assertEquals(1, count)

        job.cancel()
        runCurrent()
        store.set("b", "2")
        runCurrent()

        // The listener must have been unregistered, so nothing more arrives.
        assertEquals(1, count)
    }

    @Test
    fun `withChanges starts with the current value`() = runTest {
        store.set("k", "first")
        val pref = StringPref("k", "default", store)
        assertEquals("first", pref.withChanges.first())
    }

    @Test
    fun `preference reports its default until set`() {
        val pref = StringPref("unset", "fallback", store)
        assertEquals("fallback", pref.get())
        assertFalse(pref.isSet)

        pref.set("real")
        // The same instance has to see its own write. isSet used to be a
        // constructor argument, which Prefs hardcoded to false, so get() kept
        // answering with the default no matter what had been stored.
        assertTrue(pref.isSet)
        assertEquals("real", pref.get())
        assertEquals("real", StringPref("unset", "fallback", store).get())
    }

    @Test
    fun `string set preference round trips its value`() {
        val pref = StringSetPref("s", mutableSetOf<String?>("default"), store)
        assertEquals(setOf<String?>("default"), pref.get()?.toSet())

        pref.set(mutableSetOf("written"))
        assertTrue(pref.isSet)
        assertEquals(setOf<String?>("written"), pref.get()?.toSet())
    }

    @Test
    fun `string set is defensively copied on write`() {
        val mutable = mutableSetOf<String?>("a")
        store.set("s", mutable)
        mutable.add("b")
        // The stored value must not have picked up the caller's later mutation.
        assertEquals(setOf<String?>("a"), store.getStringSet("s")?.toSet())
    }
}
