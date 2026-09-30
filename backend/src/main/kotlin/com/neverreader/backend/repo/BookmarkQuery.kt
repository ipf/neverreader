package com.neverreader.backend.repo

import androidx.sqlite.db.SimpleSQLiteQuery
import com.neverreader.backend.model.BookmarkSort
import com.neverreader.backend.model.ListFilter

/**
 * Builds the paged list query. Pure string work, deliberately separate from the
 * Room plumbing in [BookmarkRepository] so it can be tested on the JVM.
 */
internal object BookmarkQuery {

    fun build(filter: ListFilter): SimpleSQLiteQuery =
        SimpleSQLiteQuery(sql(filter), args(filter).toTypedArray())

    /**
     * The statement on its own. The bound values come from [args]; this only has
     * to emit placeholders in the same order. It used to build a parallel list
     * of arguments as it went and then throw it away, which read as though the
     * two were being kept in step.
     */
    fun sql(filter: ListFilter): String {
        return buildString {
            append("SELECT * FROM bookmarks WHERE 1=1")
            filter.unread?.let { append(" AND unread = ?") }
            filter.favorite?.let { append(" AND favorite = ?") }
            filter.tag?.let { append(" AND tagsJson LIKE ? ESCAPE '\\'") }
            filter.search?.let {
                // likePattern, not a bare "%$it%": unescaped, a % in the query
                // matches every row and _ matches any character.
                append(
                    " AND (title LIKE ? ESCAPE '\\'" +
                        " OR excerpt LIKE ? ESCAPE '\\'" +
                        " OR url LIKE ? ESCAPE '\\')",
                )
            }
            // id breaks ties. Without a total order, paging can repeat or skip
            // rows whenever two bookmarks share a timestamp or a title.
            append(" ORDER BY ").append(orderBy(filter.sort)).append(", id ASC")
        }
    }

    /** Bound values, in the order the placeholders appear in [sql]. */
    fun args(filter: ListFilter): List<Any> = buildList {
        filter.unread?.let { add(it) }
        filter.favorite?.let { add(it) }
        filter.tag?.let { add(likePattern(it)) }
        filter.search?.let {
            val pattern = likePattern(it)
            add(pattern); add(pattern); add(pattern)
        }
    }

    /**
     * A closed set of fragments. Nothing from the user is ever interpolated into
     * SQL - search terms arrive as bound arguments.
     *
     * Title sorts are NOCASE so "berlin" and "Berlin" land together.
     */
    fun orderBy(sort: BookmarkSort): String = when (sort) {
        BookmarkSort.NEWEST -> "createdAt DESC"
        BookmarkSort.OLDEST -> "createdAt ASC"
        BookmarkSort.TITLE_ASC -> "title COLLATE NOCASE ASC"
        BookmarkSort.TITLE_DESC -> "title COLLATE NOCASE DESC"
    }

    /**
     * Wraps a term in `%` with the LIKE metacharacters escaped, so a user typing
     * `100%` or `a_b` searches for that literal text.
     */
    fun likePattern(term: String): String =
        "%" + term
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_") + "%"
}
