package com.neverreader.repository

import android.content.Context
import androidx.paging.PagingData
import com.neverreader.backend.DataGraph
import dagger.hilt.android.qualifiers.ApplicationContext
import com.neverreader.backend.model.Annotation
import com.neverreader.backend.model.Bookmark
import com.neverreader.backend.model.ListFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class TagCount(val name: String, val count: Int)

/**
 * Entry point for the app's bookmark data, backed by the :backend module.
 */
@Singleton
class BookmarkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val repo by lazy { DataGraph.bookmarkRepository(context) }

    fun bookmarks(filter: ListFilter): Flow<PagingData<Bookmark>> = repo.bookmarks(filter)

    fun bookmark(id: String): Flow<Bookmark?> = repo.bookmark(id)

    fun bookmarkByUrl(url: String): Flow<Bookmark?> = repo.bookmarkByUrl(url)

    suspend fun bookmarkOnce(id: String): Bookmark? = repo.bookmarkOnce(id)

    suspend fun bookmarkByUrlOnce(url: String): Bookmark? = repo.bookmarkByUrlOnce(url)

    fun tags(): Flow<List<TagCount>> = repo.tags().map { list -> list.map { TagCount(it.name, it.count) } }

    fun annotations(bookmarkId: String): Flow<List<Annotation>> = repo.annotations(bookmarkId)

    suspend fun setArchived(id: String, archived: Boolean) = repo.setArchived(id, archived)

    suspend fun setFavorite(id: String, favorite: Boolean) = repo.setFavorite(id, favorite)

    suspend fun delete(id: String) = repo.delete(id)

    suspend fun add(url: String, title: String?): Bookmark = repo.add(url, title)

    suspend fun addTags(bookmarkId: String, names: List<String>) = repo.addTags(bookmarkId, names)

    suspend fun removeTag(bookmarkId: String, name: String) = repo.removeTag(bookmarkId, name)

    suspend fun addAnnotation(annotation: Annotation): Annotation = repo.addAnnotation(annotation)

    suspend fun deleteAnnotation(bookmarkId: String, annotationId: String) =
        repo.deleteAnnotation(bookmarkId, annotationId)

    suspend fun sync(): Boolean = repo.sync()
}
