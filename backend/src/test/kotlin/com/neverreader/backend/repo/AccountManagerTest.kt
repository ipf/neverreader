package com.neverreader.backend.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.neverreader.backend.TokenCipher
import com.neverreader.backend.db.BookmarkEntity
import com.neverreader.backend.db.NeverReaderDatabase
import com.neverreader.backend.model.Account
import com.neverreader.backend.model.BackendType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AccountManager, which nothing could reach before.
 *
 * It encrypts access and refresh tokens through Tink, and Tink's
 * AndroidKeysetManager keeps its master key in the Android keystore, which
 * Robolectric does not provide. So the whole class had no tests, and it is the
 * one place a token could be written to disk in the clear.
 *
 * A reversible stand-in cipher stands in for Tink here on purpose: it makes the
 * stored bytes visibly not the plaintext, which is the property being checked.
 * The real AES path is covered by TokenCipherTest against a Tink keyset that
 * needs no keystore.
 */
@RunWith(RobolectricTestRunner::class)
class AccountManagerTest {

    private lateinit var db: NeverReaderDatabase
    private lateinit var context: Context
    private val scopes = mutableListOf<CoroutineScope>()

    /**
     * Reversal: reversible so round-trips can be checked, and it does not leave
     * the plaintext as a substring, which is the property being asserted. The
     * first attempt wrapped it in "enc(...)", which contained the token and so
     * proved nothing.
     */
    private class ReversibleCipher : TokenCipher {
        override fun encrypt(plaintext: String): String = plaintext.reversed()
        override fun decrypt(ciphertext: String): String = ciphertext.reversed()
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NeverReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        db.close()
    }

    private fun manager(): AccountManager {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        scopes += scope
        return AccountManager(db, ReversibleCipher(), scope)
    }

    private fun account(token: String = "access-token") = Account(
        backendType = BackendType.READECK,
        serverUrl = "https://readeck.example",
        username = "demo",
        accessToken = token,
        refreshToken = "refresh-token",
        clientId = "client",
        clientSecret = "secret",
    )

    @Test
    fun `no account before one is saved`() = runTest {
        assertNull(manager().active())
        assertEquals(SyncState(0L, 0L), manager().syncState())
    }

    /**
     * The point of the whole exercise: what lands in the database must not be
     * the token. Read straight from the row rather than through the manager, so
     * the encryption cannot hide behind a decrypt on the way out.
     */
    @Test
    fun `tokens are not stored in the clear`() = runTest {
        manager().save(account(token = "super-secret-access"))

        val row = db.accountDao().get()!!
        assertNotEquals("super-secret-access", row.accessToken)
        assertNotEquals("refresh-token", row.refreshToken)
        assertTrue(
            row.accessToken!!.contains("super-secret-access").not(),
            "the access token is readable in the stored row",
        )
    }

    @Test
    fun `a saved account reads back intact`() = runTest {
        val manager = manager()
        manager.save(account())

        val read = manager.active()!!
        assertEquals("access-token", read.accessToken)
        assertEquals("refresh-token", read.refreshToken)
        assertEquals("https://readeck.example", read.serverUrl)
        assertEquals("demo", read.username)
        assertEquals("client", read.clientId)
        assertEquals(BackendType.READECK, read.backendType)
    }

    @Test
    fun `a saved account observes`() = runTest {
        manager().save(account())

        val emitted = manager().observe().first()
        assertEquals("access-token", emitted?.accessToken)
    }

    /** Saving is a fresh sign-in, so it starts the sync cursors from zero. */
    @Test
    fun `saving resets the sync cursors`() = runTest {
        val manager = manager()
        manager.save(account())
        manager.updateSyncState(lastSyncAt = 5_000L, lastFullSyncAt = 6_000L)
        assertEquals(SyncState(5_000L, 6_000L), manager.syncState())

        manager.save(account(token = "second"))

        assertEquals(SyncState(0L, 0L), manager.syncState())
    }

    @Test
    fun `sync state advances`() = runTest {
        val manager = manager()
        manager.save(account())

        manager.updateSyncState(lastSyncAt = 111L, lastFullSyncAt = 222L)

        assertEquals(SyncState(111L, 222L), manager.syncState())
    }

    /**
     * update() is the token-refresh path, and it must not move the sync
     * cursors - a refresh is not a sync, and resetting them would re-download
     * the whole library every time a token expired.
     */
    @Test
    fun `updating tokens leaves the sync cursors alone`() = runTest {
        val manager = manager()
        manager.save(account())
        manager.updateSyncState(lastSyncAt = 111L, lastFullSyncAt = 222L)

        manager.update(
            account(token = "rotated").copy(refreshToken = "rotated-refresh"),
        )

        assertEquals(SyncState(111L, 222L), manager.syncState())
        assertEquals("rotated", manager.active()?.accessToken)
    }

    @Test
    fun `updating before any account exists does nothing`() = runTest {
        manager().update(account())

        assertNull(manager().active())
    }

    /**
     * Logging out has to clear the bookmarks too, or the next account to sign
     * in sees the previous one's library - and their highlights.
     */
    @Test
    fun `logging out clears the account and its bookmarks`() = runTest {
        val manager = manager()
        manager.save(account())
        db.bookmarkDao().upsertAll(
            listOf(
                BookmarkEntity(
                    id = "b", url = "https://example.com", title = "B", excerpt = "",
                    imageUrl = "", unread = true, favorite = false, readingTimeMinutes = 0,
                    createdAt = 0L, updatedAt = 0L, tagsJson = "[]",
                ),
            ),
        )

        manager.logout()

        assertNull(manager.active())
        assertTrue(db.bookmarkDao().allIds().isEmpty())
    }
}