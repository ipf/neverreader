package com.neverreader.backend

import com.neverreader.backend.model.Annotation
import com.neverreader.backend.model.BackendCapabilities
import com.neverreader.backend.model.BackendType
import com.neverreader.backend.model.Bookmark
import com.neverreader.backend.model.ListFilter
import com.neverreader.backend.model.Tag

/**
 * A read-it-later service. Implementations map their entries into the domain model.
 */
interface Backend {
    val type: BackendType
    val capabilities: BackendCapabilities

    suspend fun listBookmarks(filter: ListFilter, limit: Int, offset: Int): List<Bookmark>
    suspend fun fetchArticleHtml(id: String): String
    suspend fun addBookmark(url: String, title: String?): Bookmark
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun setFavorite(id: String, favorite: Boolean)
    suspend fun deleteBookmark(id: String)

    suspend fun listTags(): List<Tag>
    suspend fun addTags(bookmarkId: String, names: List<String>)
    suspend fun removeTag(bookmarkId: String, name: String)

    suspend fun listAnnotations(bookmarkId: String): List<Annotation>
    suspend fun addAnnotation(bookmarkId: String, annotation: Annotation): Annotation
    suspend fun deleteAnnotation(bookmarkId: String, annotationId: String)

    /** Entries created or updated on the server after [since] (epoch millis). */
    suspend fun changedBookmarks(since: Long): List<Bookmark>

    /** Ids deleted on the server after [since], or null when the backend cannot report deletions. */
    suspend fun deletedBookmarkIds(since: Long): List<String>?
}
