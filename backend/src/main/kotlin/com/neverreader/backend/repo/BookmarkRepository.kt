package com.neverreader.backend.repo

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.sqlite.db.SimpleSQLiteQuery
import com.neverreader.backend.Backend
import com.neverreader.backend.Backends
import com.neverreader.backend.db.MutationType
import com.neverreader.backend.db.NeverReaderDatabase
import com.neverreader.backend.db.PendingMutationEntity
import com.neverreader.backend.db.TagCount
import com.neverreader.backend.model.Annotation
import com.neverreader.backend.model.Bookmark
import com.neverreader.backend.model.ListFilter
import com.neverreader.backend.sync.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
private val json = Json

private val PAGE = 30

// A deletion made directly on the server is only noticed by a full refresh,
// because Wallabag's updatedSince has no tombstone. That is how often one runs.
private val FULL_REFRESH_MS = 7L * 24 * 60 * 60 * 1000

class BookmarkRepository(
    private val context: Context,
    private val db: NeverReaderDatabase,
    private val accounts: AccountManager,
) {

    fun bookmarks(filter: ListFilter): Flow<PagingData<Bookmark>> =
        Pager(PagingConfig(pageSize = PAGE)) {
            db.bookmarkDao().paged(queryFor(filter))
        }.flow.map { paging -> paging.map { it.toDomain() } }

    fun bookmark(id: String): Flow<Bookmark?> =
        db.bookmarkDao().byId(id).map { it?.toDomain() }

    fun bookmarkByUrl(url: String): Flow<Bookmark?> =
        db.bookmarkDao().byUrl(url).map { it?.toDomain() }

    suspend fun bookmarkOnce(id: String): Bookmark? = db.bookmarkDao().get(id)?.toDomain()

    suspend fun bookmarkByUrlOnce(url: String): Bookmark? = db.bookmarkDao().getByUrl(url)?.toDomain()

    fun tags(): Flow<List<TagCount>> = db.bookmarkDao().tagCounts()

    fun annotations(bookmarkId: String): Flow<List<Annotation>> =
        db.annotationDao().byBookmarkFlow(bookmarkId).map { list -> list.map { it.toDomain() } }

    fun pendingCount(): Flow<Int> = db.pendingMutationDao().count()

    suspend fun add(url: String, title: String?): Bookmark {
        val bookmark = currentBackend().addBookmark(url, title)
        store(listOf(bookmark))
        return bookmark
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        db.bookmarkDao().setUnread(id, !archived)
        enqueueMutation(id, MutationType.ARCHIVE, archived.toString())
    }

    suspend fun setFavorite(id: String, favorite: Boolean) {
        db.bookmarkDao().setFavorite(id, favorite)
        enqueueMutation(id, MutationType.FAVORITE, favorite.toString())
    }

    suspend fun delete(id: String) {
        db.bookmarkDao().deleteByIds(listOf(id))
        db.annotationDao().deleteByBookmark(id)
        db.pendingMutationDao().clear(id, MutationType.ARCHIVE)
        db.pendingMutationDao().clear(id, MutationType.FAVORITE)
        enqueueMutation(id, MutationType.DELETE, null)
    }

    suspend fun addTags(bookmarkId: String, names: List<String>) {
        enqueueMutation(bookmarkId, MutationType.TAG_ADD, json.encodeToString(ListSerializer(String.serializer()), names))
    }

    suspend fun removeTag(bookmarkId: String, name: String) {
        enqueueMutation(bookmarkId, MutationType.TAG_REMOVE, name)
    }

    suspend fun addAnnotation(annotation: Annotation): Annotation {
        val created = currentBackend().addAnnotation(annotation.bookmarkId, annotation)
        db.annotationDao().upsertAll(listOf(created.toEntity()))
        return created
    }

    suspend fun deleteAnnotation(bookmarkId: String, annotationId: String) {
        db.annotationDao().delete(annotationId)
        db.pendingMutationDao().insert(
            PendingMutationEntity(
                bookmarkId = bookmarkId,
                type = MutationType.ANNOTATION_DELETE,
                payload = annotationId,
                createdAt = System.currentTimeMillis(),
            ),
        )
        SyncWorker.enqueueNow(context)
    }

    suspend fun sync(): Boolean {
        val account = accounts.active() ?: return false
        val backend = Backends.create(account, onTokensRefreshed = { accounts.update(it) })
        pushPending(backend)
        pullDown(backend)
        return true
    }

    private suspend fun currentBackend(): Backend {
        val account = accounts.active() ?: error("No active account")
        return Backends.create(account, onTokensRefreshed = { accounts.update(it) })
    }

    private suspend fun enqueueMutation(bookmarkId: String, type: String, payload: String?) {
        db.pendingMutationDao().clear(bookmarkId, type)
        db.pendingMutationDao().insert(
            PendingMutationEntity(
                bookmarkId = bookmarkId,
                type = type,
                payload = payload,
                createdAt = System.currentTimeMillis(),
            ),
        )
        SyncWorker.enqueueNow(context)
    }

    private suspend fun pushPending(backend: Backend) {
        val mutations = db.pendingMutationDao().all()
        val applied = mutableListOf<Long>()
        try {
            for (mutation in mutations) {
                apply(backend, mutation)
                applied.add(mutation.id)
            }
        } finally {
            if (applied.isNotEmpty()) db.pendingMutationDao().delete(applied)
        }
    }

    private suspend fun apply(backend: Backend, mutation: PendingMutationEntity) {
        when (mutation.type) {
            MutationType.ARCHIVE -> backend.setArchived(mutation.bookmarkId, mutation.payload!!.toBoolean())
            MutationType.FAVORITE -> backend.setFavorite(mutation.bookmarkId, mutation.payload!!.toBoolean())
            MutationType.DELETE -> backend.deleteBookmark(mutation.bookmarkId)
            MutationType.TAG_ADD -> backend.addTags(mutation.bookmarkId, json.decodeFromString(mutation.payload!!))
            MutationType.TAG_REMOVE -> backend.removeTag(mutation.bookmarkId, mutation.payload!!)
            MutationType.ANNOTATION_ADD -> {
                val local = json.decodeFromString<Annotation>(mutation.payload!!)
                val created = backend.addAnnotation(mutation.bookmarkId, local)
                db.annotationDao().delete(local.id)
                db.annotationDao().upsertAll(listOf(created.toEntity()))
            }
            MutationType.ANNOTATION_DELETE -> backend.deleteAnnotation(mutation.bookmarkId, mutation.payload!!)
        }
    }

    private suspend fun pullDown(backend: Backend) {
        val state = accounts.syncState()
        val now = System.currentTimeMillis()
        if (state.lastFullSyncAt == 0L || now - state.lastFullSyncAt > FULL_REFRESH_MS) {
            // The full refresh is the only chance to notice a deletion, because
            // Wallabag's updatedSince cannot report one. It used to just upsert
            // every page, which meant an article deleted on the server stayed in
            // the local list forever - the comments here and in WallabagAdapter
            // both claimed the refresh handled it and it did not.
            val seen = mutableSetOf<String>()
            var offset = 0
            while (true) {
                val page = backend.listBookmarks(ListFilter(), PAGE, offset)
                seen += page.map { it.id }
                store(page)
                if (page.size < PAGE) break
                offset += PAGE
            }
            purgeMissing(db, seen)
            accounts.updateSyncState(lastSyncAt = now, lastFullSyncAt = now)
        } else {
            val changed = backend.changedBookmarks(state.lastSyncAt)
            store(changed)
            backend.deletedBookmarkIds(state.lastSyncAt)?.let { ids -> removeAll(db, ids) }
            accounts.updateSyncState(lastSyncAt = now)
        }
    }

    private suspend fun store(bookmarks: List<Bookmark>) {
        if (bookmarks.isEmpty()) return
        val ids = bookmarks.map { it.id }
        db.bookmarkDao().deleteTagLinks(ids)
        db.bookmarkDao().upsertAll(bookmarks.map { it.toEntity() })
        db.bookmarkDao().insertTagLinks(bookmarks.flatMap { b -> b.tags.map { com.neverreader.backend.db.BookmarkTagEntity(b.id, it) } })
    }

    private fun queryFor(filter: ListFilter): SimpleSQLiteQuery = BookmarkQuery.build(filter)
}

/**
 * Drop everything stored locally that a full refresh did not see.
 *
 * Only the full refresh can do this, since a delta sync asks the server what
 * changed and Wallabag cannot answer that with a tombstone. Compared by id, so
 * an article that was merely re-favorited, archived or re-tagged on the server
 * between refreshes survives.
 *
 * Internal rather than private so it can be tested against a real database:
 * AccountManager needs Tink and the Android keystore, which Robolectric does
 * not provide, and this is the part worth pinning anyway.
 */
internal suspend fun purgeMissing(db: NeverReaderDatabase, seen: Set<String>) =
    removeAll(db, db.bookmarkDao().allIds() - seen)

/**
 * Remove bookmarks and everything hanging off them.
 *
 * Annotations are keyed by their own id rather than foreign-keyed to the
 * bookmark, so nothing removes them for us. Both sync paths have to do it: a
 * delta sync that dropped the bookmark left orphaned highlights, and so did a
 * full refresh before it purged at all.
 */
internal suspend fun removeAll(db: NeverReaderDatabase, ids: List<String>) {
    if (ids.isEmpty()) return
    db.bookmarkDao().deleteByIds(ids)
    db.bookmarkDao().deleteTagLinks(ids)
    ids.forEach { db.annotationDao().deleteByBookmark(it) }
}

private fun com.neverreader.backend.db.BookmarkEntity.toDomain() = Bookmark(
    id = id,
    url = url,
    title = title,
    excerpt = excerpt,
    imageUrl = imageUrl,
    unread = unread,
    favorite = favorite,
    readingTimeMinutes = readingTimeMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tags = json.decodeFromString(ListSerializer(String.serializer()), tagsJson),
)

private fun Bookmark.toEntity() = com.neverreader.backend.db.BookmarkEntity(
    id = id,
    url = url,
    title = title,
    excerpt = excerpt,
    imageUrl = imageUrl,
    unread = unread,
    favorite = favorite,
    readingTimeMinutes = readingTimeMinutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tagsJson = json.encodeToString(ListSerializer(String.serializer()), tags),
)

private fun com.neverreader.backend.db.AnnotationEntity.toDomain() = Annotation(
    id = id,
    bookmarkId = bookmarkId,
    text = text,
    comment = comment,
    startSelector = startSelector,
    startOffset = startOffset,
    endSelector = endSelector,
    endOffset = endOffset,
    createdAt = createdAt,
)

private fun Annotation.toEntity() = com.neverreader.backend.db.AnnotationEntity(
    id = id,
    bookmarkId = bookmarkId,
    text = text,
    comment = comment,
    startSelector = startSelector,
    startOffset = startOffset,
    endSelector = endSelector,
    endOffset = endOffset,
    createdAt = createdAt,
)
