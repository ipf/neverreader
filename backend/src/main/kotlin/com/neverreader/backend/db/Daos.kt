package com.neverreader.backend.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @RawQuery(observedEntities = [BookmarkEntity::class])
    fun paged(query: SupportSQLiteQuery): PagingSource<Int, BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    fun byId(id: String): Flow<BookmarkEntity?>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun get(id: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    fun byUrl(url: String): Flow<BookmarkEntity?>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): BookmarkEntity?

    @Upsert
    suspend fun upsertAll(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM bookmarks")
    suspend fun clear()

    /**
     * Every id currently stored. A full refresh compares this against what the
     * server returned, so it needs the whole set rather than a page.
     */
    @Query("SELECT id FROM bookmarks")
    suspend fun allIds(): List<String>

    @Query("UPDATE bookmarks SET unread = :unread WHERE id = :id")
    suspend fun setUnread(id: String, unread: Boolean)

    @Query("UPDATE bookmarks SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("SELECT tagName AS name, COUNT(*) AS count FROM bookmark_tags GROUP BY tagName ORDER BY COUNT(*) DESC")
    fun tagCounts(): Flow<List<TagCount>>

    @Insert
    suspend fun insertTagLinks(rows: List<BookmarkTagEntity>)

    @Query("DELETE FROM bookmark_tags WHERE bookmarkId IN (:ids)")
    suspend fun deleteTagLinks(ids: List<String>)

    @Query("DELETE FROM bookmark_tags")
    suspend fun clearTagLinks()
}

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE bookmarkId = :bookmarkId ORDER BY createdAt")
    suspend fun byBookmark(bookmarkId: String): List<AnnotationEntity>

    @Query("SELECT * FROM annotations WHERE bookmarkId = :bookmarkId ORDER BY createdAt")
    fun byBookmarkFlow(bookmarkId: String): Flow<List<AnnotationEntity>>

    @Upsert
    suspend fun upsertAll(annotations: List<AnnotationEntity>)

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM annotations WHERE bookmarkId = :bookmarkId")
    suspend fun deleteByBookmark(bookmarkId: String)

    @Query("DELETE FROM annotations")
    suspend fun deleteAll()
}

@Dao
interface PendingMutationDao {
    @Insert
    suspend fun insert(mutation: PendingMutationEntity)

    @Query("SELECT * FROM pending_mutations ORDER BY id")
    suspend fun all(): List<PendingMutationEntity>

    @Query("DELETE FROM pending_mutations WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("DELETE FROM pending_mutations WHERE bookmarkId = :bookmarkId AND type = :type")
    suspend fun clear(bookmarkId: String, type: String)

    @Query("DELETE FROM pending_mutations")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM pending_mutations")
    fun count(): Flow<Int>
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM account WHERE id = 1")
    suspend fun get(): AccountEntity?

    @Query("SELECT * FROM account WHERE id = 1")
    fun observe(): Flow<AccountEntity?>

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Query("DELETE FROM account")
    suspend fun clear()
}
