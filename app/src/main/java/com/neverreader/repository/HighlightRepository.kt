package com.neverreader.repository

import com.neverreader.backend.DataGraph
import com.neverreader.backend.model.Annotation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HighlightRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) {

    private val repo by lazy { DataGraph.bookmarkRepository(context) }

    fun getHighlightsFlow(bookmarkId: String): Flow<List<Annotation>> = repo.annotations(bookmarkId)

    suspend fun addHighlight(annotation: Annotation): Annotation = repo.addAnnotation(annotation)

    suspend fun deleteHighlight(bookmarkId: String, annotationId: String) =
        repo.deleteAnnotation(bookmarkId, annotationId)
}
