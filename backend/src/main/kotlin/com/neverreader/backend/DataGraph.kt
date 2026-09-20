package com.neverreader.backend

import android.content.Context
import com.neverreader.backend.db.NeverReaderDatabase
import com.neverreader.backend.repo.AccountManager
import com.neverreader.backend.repo.BookmarkRepository

/**
 * Hand-rolled singleton graph for the backend module (no DI framework).
 */
object DataGraph {

    @Volatile private var db: NeverReaderDatabase? = null
    @Volatile private var accounts: AccountManager? = null
    @Volatile private var bookmarks: BookmarkRepository? = null

    fun database(context: Context): NeverReaderDatabase =
        db ?: synchronized(this) {
            db ?: NeverReaderDatabase.build(context.applicationContext).also { db = it }
        }

    fun accountManager(context: Context): AccountManager =
        accounts ?: synchronized(this) {
            accounts ?: AccountManager(context.applicationContext, database(context)).also { accounts = it }
        }

    fun bookmarkRepository(context: Context): BookmarkRepository =
        bookmarks ?: synchronized(this) {
            bookmarks ?: BookmarkRepository(
                context.applicationContext,
                database(context),
                accountManager(context),
            ).also { bookmarks = it }
        }
}
