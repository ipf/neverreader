package com.neverreader.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neverreader.backend.model.Account
import com.neverreader.backend.model.BackendType
import com.neverreader.backend.readeck.ReadeckAuth
import com.neverreader.backend.repo.AccountManager
import com.neverreader.backend.sync.SyncWorker
import com.neverreader.backend.wallabag.WallabagAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val accountManager: AccountManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val http = OkHttpClient()

    val state: StateFlow<State>
        field = MutableStateFlow<State>(State.EnterServerUrl())

    // replay=1: Success is emitted while the user is still in the browser (fragment stopped,
    // collector cancelled) — it must replay when they return.
    val events: SharedFlow<Event>
        field = MutableSharedFlow<Event>(replay = 1)

    fun onServerUrlChange(url: String) {
        state.value = State.EnterServerUrl(url = url.trim())
    }

    fun onBackendTypeChange(type: BackendType) {
        state.value = State.EnterServerUrl(url = state.value.url, backendType = type)
    }

    /**
     * Readeck: register a client, start the device flow, and poll for the token.
     */
    fun startReadeckDeviceFlow() {
        val serverUrl = state.value.url
        if (serverUrl.isBlank()) return
        state.value = State.Authorizing(serverUrl, BackendType.READECK, message = "Contacting server…")
        viewModelScope.launch {
            try {
                val clientId = withContext(Dispatchers.IO) {
                    ReadeckAuth.registerClient(serverUrl, http)
                }
                val session = withContext(Dispatchers.IO) {
                    ReadeckAuth.startDeviceFlow(serverUrl, clientId, http)
                }
                state.value = State.DeviceFlow(serverUrl, session)
                val token = withContext(Dispatchers.IO) {
                    ReadeckAuth.awaitToken(serverUrl, clientId, session, http = http)
                }
                save(Account(BackendType.READECK, serverUrl, accessToken = token, clientId = clientId))
            } catch (t: Throwable) {
                fail(t.message ?: "Authorization failed")
            }
        }
    }

    /**
     * Wallabag: OAuth2 password grant.
     */
    fun loginWallabag(username: String, password: String, clientId: String, clientSecret: String) {
        val serverUrl = state.value.url
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            fail("All fields are required")
            return
        }
        state.value = State.Authorizing(serverUrl, BackendType.WALLABAG, message = "Signing in…")
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    WallabagAuth.login(serverUrl, username, password, clientId, clientSecret, http)
                }
                save(
                    Account(
                        BackendType.WALLABAG,
                        serverUrl,
                        username = username,
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                        clientId = clientId,
                        clientSecret = clientSecret,
                    ),
                )
            } catch (t: Throwable) {
                fail(t.message ?: "Login failed")
            }
        }
    }

    private suspend fun save(account: Account) {
        accountManager.save(account)
        SyncWorker.enqueueNow(appContext)
        events.emit(Event.Success)
    }

    private fun fail(message: String) {
        state.value = State.EnterServerUrl(url = state.value.url, error = message)
    }

    sealed class State {
        abstract val url: String
        abstract val error: String?

        data class EnterServerUrl(
            override val url: String = "",
            val backendType: BackendType = BackendType.READECK,
            override val error: String? = null,
        ) : State()

        data class Authorizing(
            override val url: String,
            val backendType: BackendType,
            val message: String,
            override val error: String? = null,
        ) : State()

        data class DeviceFlow(
            override val url: String,
            val session: com.neverreader.backend.readeck.DeviceSession,
            override val error: String? = null,
        ) : State()
    }

    sealed class Event {
        data object Success : Event()
    }
}
