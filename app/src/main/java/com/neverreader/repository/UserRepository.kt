package com.neverreader.repository

import android.content.Context
import com.neverreader.backend.DataGraph
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    fun isLoggedIn(): Flow<Boolean>
}

@Singleton
class NeverReaderUserRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserRepository {

    private val accounts by lazy { DataGraph.accountManager(context) }

    override fun isLoggedIn(): Flow<Boolean> = accounts.observe().map { it != null }
}
