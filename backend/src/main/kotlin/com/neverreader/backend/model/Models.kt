package com.neverreader.backend.model

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

enum class BackendType { READECK, WALLABAG }

data class BackendCapabilities(
    val highlights: Boolean,
    val serverSearch: Boolean,
    val deltaSync: Boolean,
)

data class Bookmark(
    val id: String,
    val url: String,
    val title: String,
    val excerpt: String,
    val imageUrl: String?,
    val unread: Boolean,
    val favorite: Boolean,
    val readingTimeMinutes: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String>,
)

data class Tag(val name: String, val count: Int)

data class Annotation(
    val id: String,
    val bookmarkId: String,
    val text: String,
    val comment: String?,
    val startSelector: String,
    val startOffset: Int,
    val endSelector: String,
    val endOffset: Int,
    val createdAt: Long,
)

data class ListFilter(
    val unread: Boolean? = null,
    val favorite: Boolean? = null,
    val tag: String? = null,
    val search: String? = null,
)

sealed interface Change {
    data class Upsert(val id: String) : Change
    data class Delete(val id: String) : Change
}

data class Account(
    val backendType: BackendType,
    val serverUrl: String,
    val username: String? = null,
    val accessToken: String,
    val refreshToken: String? = null,
    val clientId: String? = null,
    val clientSecret: String? = null,
)

object Iso {
    private val withColon = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val noColon = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")

    fun toMillis(text: String): Long {
        if (text.isBlank()) return 0L
        return try {
            OffsetDateTime.parse(text, withColon).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            OffsetDateTime.parse(text, noColon).toInstant().toEpochMilli()
        }
    }

    fun fromMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).format(withColon)
}
