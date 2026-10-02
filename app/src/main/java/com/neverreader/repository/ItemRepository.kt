package com.neverreader.repository

import com.neverreader.backend.DataGraph
import com.neverreader.backend.model.Bookmark
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface ItemRepository {
    suspend fun getItemByUrl(url: String): Bookmark?
    suspend fun getItem(id: String): Bookmark?
    fun getItemFlow(id: String): Flow<Bookmark?>
    suspend fun toggleFavorite(item: Bookmark)
    suspend fun archive(item: Bookmark)
    suspend fun archive(items: List<Bookmark>)
    suspend fun unArchive(item: Bookmark)
    suspend fun unArchive(items: List<Bookmark>)
    suspend fun delete(item: Bookmark)
    suspend fun delete(items: List<Bookmark>)
    suspend fun save(url: String): Bookmark

    companion object {
        const val DEFAULT_PAGE_SIZE = 30
    }
}

@Singleton
class NeverReaderItemRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ItemRepository {

    private val repo by lazy { DataGraph.bookmarkRepository(context) }

    override suspend fun getItemByUrl(url: String): Bookmark? = repo.bookmarkByUrlOnce(url)

    override suspend fun getItem(id: String): Bookmark? = repo.bookmarkOnce(id)

    override fun getItemFlow(id: String): Flow<Bookmark?> = repo.bookmark(id)

    override suspend fun toggleFavorite(item: Bookmark) {
        repo.setFavorite(item.id, !item.favorite)
    }

    override suspend fun archive(item: Bookmark) {
        repo.setArchived(item.id, true)
    }

    override suspend fun archive(items: List<Bookmark>) {
        items.forEach { repo.setArchived(it.id, true) }
    }

    override suspend fun unArchive(item: Bookmark) {
        repo.setArchived(item.id, false)
    }

    override suspend fun unArchive(items: List<Bookmark>) {
        items.forEach { repo.setArchived(it.id, false) }
    }

    override suspend fun delete(item: Bookmark) {
        repo.delete(item.id)
    }

    override suspend fun delete(items: List<Bookmark>) {
        items.forEach { repo.delete(it.id) }
    }

    override suspend fun save(url: String): Bookmark = repo.add(url, null)
}
