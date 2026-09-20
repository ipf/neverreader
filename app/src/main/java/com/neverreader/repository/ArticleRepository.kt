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
        return Backends.create(account).fetchArticleHtml(id)
    }
}
