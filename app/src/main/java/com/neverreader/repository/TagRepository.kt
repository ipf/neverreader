package com.neverreader.repository

import com.neverreader.backend.DataGraph
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) {

    private val repo by lazy { DataGraph.bookmarkRepository(context) }

    fun tags(): Flow<List<TagCount>> = repo.tags()

    suspend fun addTags(bookmarkId: String, names: List<String>) = repo.addTags(bookmarkId, names)

    suspend fun removeTag(bookmarkId: String, name: String) = repo.removeTag(bookmarkId, name)
}
