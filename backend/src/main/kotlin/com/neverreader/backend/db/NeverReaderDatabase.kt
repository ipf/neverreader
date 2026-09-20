package com.neverreader.backend.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookmarkEntity::class,
        BookmarkTagEntity::class,
        AnnotationEntity::class,
        PendingMutationEntity::class,
        AccountEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NeverReaderDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun pendingMutationDao(): PendingMutationDao
    abstract fun accountDao(): AccountDao

    companion object {
        fun build(context: Context): NeverReaderDatabase =
            Room.databaseBuilder(context, NeverReaderDatabase::class.java, "neverreader.db")
                .build()
    }
}
