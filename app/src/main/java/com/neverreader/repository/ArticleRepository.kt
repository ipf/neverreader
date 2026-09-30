package com.neverreader.repository

import com.neverreader.backend.Backends
import com.neverreader.backend.repo.AccountManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val accounts: AccountManager,
) {

    suspend fun getArticleHtml(
        id: String,
    ): String {
        val account = accounts.active() ?: error("No active account")
        // Without the callback, a Wallabag access token that expires mid-read is
        // refreshed in memory and thrown away: the reader would refresh again on
        // every article, and a failed refresh surfaces as a load error rather
        // than a retry. The sync path already passes this.
        return Backends.create(account, onTokensRefreshed = { accounts.update(it) })
            .fetchArticleHtml(id)
    }
}
