package com.neverreader.backend.repo

import android.content.Context
import com.neverreader.backend.TinkTokenCipher
import com.neverreader.backend.TokenCipher
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
// The primary constructor is internal rather than private because it carries the
// test seams; see the injected constructor below.
class AccountManager internal constructor(
    private val db: NeverReaderDatabase,
    private val cipher: TokenCipher,
    /**
     * Where the activeCached mirror is kept up to date. Injectable so a test can
     * cancel it: the collector in init never ends on its own, and a leaked one
     * outlives the test that made it.
     */
    private val scope: CoroutineScope,
) : Accounts {

    /**
     * Dagger ignores Kotlin default arguments, so the injectable constructor
     * keeps only the two dependencies it can actually supply; the cipher and the
     * scope are test seams.
     */
    @javax.inject.Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
        db: NeverReaderDatabase,
    ) : this(
        db = db,
        cipher = TinkTokenCipher(context),
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )


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
            accessToken = cipher.encrypt(account.accessToken),
            refreshToken = account.refreshToken?.let { cipher.encrypt(it) },
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
        accessToken = accessToken?.let { cipher.decrypt(it) }.orEmpty(),
        refreshToken = refreshToken?.let { cipher.decrypt(it) },
        clientId = clientId,
        clientSecret = clientSecret,
    )

    private fun Account.toEntity(lastSyncAt: Long, lastFullSyncAt: Long) = com.neverreader.backend.db.AccountEntity(
        backendType = backendType.name,
        serverUrl = serverUrl,
        username = username,
        accessToken = cipher.encrypt(accessToken),
        refreshToken = refreshToken?.let { cipher.encrypt(it) },
        clientId = clientId,
        clientSecret = clientSecret,
        lastSyncAt = lastSyncAt,
        lastFullSyncAt = lastFullSyncAt,
    )
}
