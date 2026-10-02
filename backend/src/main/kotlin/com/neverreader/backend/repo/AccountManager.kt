package com.neverreader.backend.repo

import android.content.Context
import com.neverreader.backend.TokenCrypto
import com.neverreader.backend.db.NeverReaderDatabase
import com.neverreader.backend.model.Account
import com.neverreader.backend.model.BackendType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class SyncState(val lastSyncAt: Long, val lastFullSyncAt: Long)

/**
 * The part of [AccountManager] the repositories depend on.
 *
 * Extracted so the sync path can be tested. AccountManager decrypts tokens
 * through Tink and the Android keystore, neither of which Robolectric provides,
 * so taking it as a concrete class left every caller of sync() untestable.
 */
interface Accounts {
    suspend fun active(): Account?
    suspend fun syncState(): SyncState
    suspend fun update(account: Account)
    suspend fun updateSyncState(lastSyncAt: Long? = null, lastFullSyncAt: Long? = null)
}

@javax.inject.Singleton
class AccountManager @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val db: NeverReaderDatabase,
) : Accounts {

    private val aead by lazy { TokenCrypto.aead(context) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Quick sync access for UI checks; kept fresh by observe() collection.
    @Volatile
    var activeCached: Account? = null
        private set

    init {
        scope.launch {
            observe().collect { activeCached = it }
        }
    }

    fun observe(): Flow<Account?> = db.accountDao().observe().map { it?.toAccount() }

    override suspend fun active(): Account? = db.accountDao().get()?.toAccount()

    override suspend fun syncState(): SyncState {
        val current = db.accountDao().get() ?: return SyncState(0L, 0L)
        return SyncState(current.lastSyncAt, current.lastFullSyncAt)
    }

    suspend fun save(account: Account) {
        db.accountDao().upsert(account.toEntity(lastSyncAt = 0L, lastFullSyncAt = 0L))
    }

    override suspend fun update(account: Account) {
        val current = db.accountDao().get() ?: return
        db.accountDao().upsert(current.copy(
            accessToken = TokenCrypto.encrypt(aead, account.accessToken),
            refreshToken = account.refreshToken?.let { TokenCrypto.encrypt(aead, it) },
        ))
    }

    override suspend fun updateSyncState(lastSyncAt: Long?, lastFullSyncAt: Long?) {
        val current = db.accountDao().get() ?: return
        db.accountDao().upsert(current.copy(
            lastSyncAt = lastSyncAt ?: current.lastSyncAt,
            lastFullSyncAt = lastFullSyncAt ?: current.lastFullSyncAt,
        ))
    }

    suspend fun logout() {
        db.accountDao().clear()
        db.bookmarkDao().clear()
        db.bookmarkDao().clearTagLinks()
        db.annotationDao().deleteAll()
        db.pendingMutationDao().clearAll()
    }

    private fun com.neverreader.backend.db.AccountEntity.toAccount() = Account(
        backendType = BackendType.valueOf(backendType),
        serverUrl = serverUrl,
        username = username,
        accessToken = accessToken?.let { TokenCrypto.decrypt(aead, it) }.orEmpty(),
        refreshToken = refreshToken?.let { TokenCrypto.decrypt(aead, it) },
        clientId = clientId,
        clientSecret = clientSecret,
    )

    private fun Account.toEntity(lastSyncAt: Long, lastFullSyncAt: Long) = com.neverreader.backend.db.AccountEntity(
        backendType = backendType.name,
        serverUrl = serverUrl,
        username = username,
        accessToken = TokenCrypto.encrypt(aead, accessToken),
        refreshToken = refreshToken?.let { TokenCrypto.encrypt(aead, it) },
        clientId = clientId,
        clientSecret = clientSecret,
        lastSyncAt = lastSyncAt,
        lastFullSyncAt = lastFullSyncAt,
    )
}
