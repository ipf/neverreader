package com.neverreader.backend

import com.neverreader.backend.model.Account
import com.neverreader.backend.model.BackendType
import com.neverreader.backend.readeck.ReadeckAdapter
import com.neverreader.backend.wallabag.WallabagAdapter

object Backends {
    fun create(account: Account, onTokensRefreshed: suspend (Account) -> Unit = {}): Backend =
        when (account.backendType) {
            BackendType.READECK -> ReadeckAdapter(account.serverUrl, account.clientId.orEmpty(), account.accessToken)
            BackendType.WALLABAG -> WallabagAdapter(account, onTokensRefreshed = onTokensRefreshed)
        }
}
