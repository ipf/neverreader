package com.neverreader.backend.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val excerpt: String,
    val imageUrl: String?,
    val unread: Boolean,
    val favorite: Boolean,
    val readingTimeMinutes: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val tagsJson: String,
)

@Entity(tableName = "bookmark_tags", primaryKeys = ["bookmarkId", "tagName"])
data class BookmarkTagEntity(val bookmarkId: String, val tagName: String)

data class TagCount(val name: String, val count: Int)

@Entity(tableName = "annotations", indices = [Index("bookmarkId")])
data class AnnotationEntity(
    @PrimaryKey val id: String,
    val bookmarkId: String,
    val text: String,
    val comment: String?,
    val startSelector: String,
    val startOffset: Int,
    val endSelector: String,
    val endOffset: Int,
    val createdAt: Long,
)

@Entity(tableName = "pending_mutations", indices = [Index("bookmarkId")])
data class PendingMutationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookmarkId: String,
    val type: String,
    val payload: String?,
    val createdAt: Long,
)

@Entity(tableName = "account")
data class AccountEntity(
    @PrimaryKey val id: Int = 1,
    val backendType: String,
    val serverUrl: String,
    val username: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val clientId: String?,
    val clientSecret: String?,
    val lastSyncAt: Long,
    val lastFullSyncAt: Long,
)

object MutationType {
    const val ARCHIVE = "ARCHIVE"
    const val FAVORITE = "FAVORITE"
    const val DELETE = "DELETE"
    const val TAG_ADD = "TAG_ADD"
    const val TAG_REMOVE = "TAG_REMOVE"
    const val ANNOTATION_ADD = "ANNOTATION_ADD"
    const val ANNOTATION_DELETE = "ANNOTATION_DELETE"
}
