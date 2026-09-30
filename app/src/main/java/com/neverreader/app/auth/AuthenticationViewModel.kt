package com.neverreader.app.auth

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neverreader.backend.model.Account
import com.neverreader.backend.model.BackendType
import com.neverreader.backend.readeck.ReadeckAuth
import com.neverreader.backend.repo.AccountManager
import com.neverreader.backend.sync.SyncWorker
import com.neverreader.backend.wallabag.WallabagAuth
import com.neverreader.util.android.Clipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val clipboard: Clipboard,
    private val savedState: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val http = OkHttpClient()

    /**
     * Survives the process being killed while the browser is in front.
     *
     * The redirect can only be completed with the verifier that produced the
     * code, and the browser is exactly the kind of foreground app that gets the
     * app killed behind it. In a field on the ViewModel both would be gone by
     * the time the redirect arrived.
     */
    private var pendingServerUrl: String?
        get() = savedState[KEY_SERVER_URL]
        set(value) {
            savedState[KEY_SERVER_URL] = value
        }
    private var pendingVerifier: String?
        get() = savedState[KEY_VERIFIER]
        set(value) {
            savedState[KEY_VERIFIER] = value
        }
    private var pendingState: String?
        get() = savedState[KEY_STATE]
        set(value) {
            savedState[KEY_STATE] = value
        }

    /** Installed versionName, as Readeck records against the registered client. */
    private fun appVersion(): String = runCatching {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
    }.getOrNull().orEmpty().ifBlank { "0.0.0" }

    val state: StateFlow<State>
        field = MutableStateFlow<State>(
            if (pendingState != null) {
                State.AwaitingRedirect(pendingServerUrl.orEmpty())
            } else {
                State.EnterServerUrl()
            }
        )

    // replay=1: Success is emitted while the user is still in the browser (fragment stopped,
    // collector cancelled) — it must replay when they return.
    val events: SharedFlow<Event>
        field = MutableSharedFlow<Event>(replay = 1)

    fun onServerUrlChange(url: String) {
        state.value = State.EnterServerUrl(
            url = url.trim(),
            backendType = state.value.backendType,
            error = state.value.error,
        )
    }

    fun onBackendTypeChange(type: BackendType) {
        state.value = State.EnterServerUrl(url = state.value.url, backendType = type)
    }

    /**
     * Readeck: register a client and hand the user the authorization page.
     *
     * The browser redirects straight back to [AuthenticationActivity] once they
     * approve, so there is nothing left to do here and nothing to come back and
     * tap - which is what the device flow needed, since it can only be polled
     * for.
     */
    fun startReadeckAuthorization() {
        val serverUrl = state.value.url
        if (serverUrl.isBlank()) return
        state.value = State.Authorizing(serverUrl, BackendType.READECK, message = "Contacting server…")
        viewModelScope.launch {
            try {
                val pending = withContext(Dispatchers.IO) {
                    ReadeckAuth.startAuthorization(serverUrl, appVersion(), http = http)
                }
                pendingServerUrl = serverUrl
                pendingVerifier = pending.codeVerifier
                pendingState = pending.state
                state.value = State.AwaitingRedirect(serverUrl, browserUrl = pending.authorizeUrl)
                events.emit(Event.OpenBrowser(pending.authorizeUrl))
            } catch (t: Throwable) {
                fail(t.message ?: "Authorization failed")
            }
        }
    }

    /**
     * No browser could be launched. The pending state is dropped so the retry
     * is not left waiting for a redirect that can never arrive.
     */
    fun onBrowserUnavailable() {
        clearPending()
        fail("No browser is available to sign in with. Use the code instead.")
    }

    /**
     * Readeck: the fallback sign-in for a device with no browser to redirect.
     * Registers a client and polls, so the user types a code instead.
     */
    fun startReadeckDeviceFlow() {
        val serverUrl = state.value.url
        if (serverUrl.isBlank()) return
        clearPending()
        state.value = State.Authorizing(serverUrl, BackendType.READECK, message = "Contacting server…")
        viewModelScope.launch {
            try {
                val clientId = withContext(Dispatchers.IO) {
                    ReadeckAuth.registerClient(serverUrl, appVersion(), http = http)
                }
                val session = withContext(Dispatchers.IO) {
                    ReadeckAuth.startDeviceFlow(serverUrl, clientId, http)
                }
                state.value = State.DeviceFlow(serverUrl, session, BackendType.READECK)
                val token = ReadeckAuth.awaitToken(serverUrl, clientId, session, http = http)
                save(Account(BackendType.READECK, serverUrl, accessToken = token, clientId = clientId))
            } catch (t: Throwable) {
                fail(t.message ?: "Authorization failed")
            }
        }
    }

    /**
     * Readeck sent the browser back. Either finish the exchange or explain why
     * it cannot.
     *
     * The state check comes first and unconditionally: it is what ties the
     * redirect to the request this app started. Without it, any app able to
     * send an intent here could hand over a code of its own.
     */
    fun onAuthorizationRedirect(uri: Uri) {
        val expectedState = pendingState
        if (expectedState == null) return

        val returnedState = uri.getQueryParameter(STATE_PARAM)
        if (returnedState != expectedState) {
            clearPending()
            fail("The sign-in response did not match this request. Try again.")
            return
        }

        val error = uri.getQueryParameter(ERROR_PARAM)
        if (error != null) {
            clearPending()
            val description = uri.getQueryParameter(ERROR_DESCRIPTION_PARAM)
            fail(
                when (error) {
                    "access_denied" -> "Sign-in was declined in the browser."
                    else -> description ?: "The server refused the sign-in ($error)."
                }
            )
            return
        }

        val code = uri.getQueryParameter(CODE_PARAM)
        val serverUrl = pendingServerUrl
        val verifier = pendingVerifier
        if (code == null || serverUrl == null || verifier == null) {
            clearPending()
            fail("The browser came back without an authorization code. Try again.")
            return
        }

        clearPending()
        state.value = State.Authorizing(serverUrl, BackendType.READECK, message = "Finishing sign-in…")
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    ReadeckAuth.exchangeCode(serverUrl, code, verifier, http)
                }
                // The client is not needed after this: the token is all that
                // goes to /oauth/revoke, and Readeck's access tokens do not
                // expire, so there is no refresh to store a secret for.
                save(Account(BackendType.READECK, serverUrl, accessToken = token))
            } catch (t: Throwable) {
                fail(t.message ?: "Could not complete sign-in")
            }
        }
    }

    /**
     * Wallabag: OAuth2 password grant.
     *
     * The client id and secret are asked for rather than defaulted. This used to
     * send a hardcoded "wallabag"/"wallabag", on the assumption that instances have
     * Wallabag's own public client registered. They do not, and it could not work
     * if they did: Wallabag resolves a client by the public id "<row id>_<random
     * id>", and findClientByPublicId returns null for any id without that
     * underscore, so a bare name never matches anything. The user copies the
     * credentials from their own Wallabag's API clients page instead.
     */
    fun loginWallabag(
        username: String,
        password: String,
        clientId: String,
        clientSecret: String,
    ) {
        val serverUrl = state.value.url
        if (listOf(serverUrl, username, password, clientId, clientSecret)
                .any { it.isBlank() }
        ) {
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

    private fun clearPending() {
        pendingServerUrl = null
        pendingVerifier = null
        pendingState = null
    }

    private suspend fun save(account: Account) {
        accountManager.save(account)
        SyncWorker.enqueueNow(appContext)
        // The url the user just signed in with is often still on the clipboard.
        // Record it as seen so the "save this url?" prompt does not greet them
        // on the list screen.
        clipboard.markCurrentUrlAsSeen()
        events.emit(Event.Success)
    }

    private fun fail(message: String) {
        state.value = State.EnterServerUrl(
            url = state.value.url,
            backendType = state.value.backendType,
            error = message,
        )
    }

    sealed class State {
        abstract val url: String
        abstract val backendType: BackendType
        abstract val error: String?

        data class EnterServerUrl(
            override val url: String = "",
            override val backendType: BackendType = BackendType.READECK,
            override val error: String? = null,
        ) : State()

        data class Authorizing(
            override val url: String,
            override val backendType: BackendType,
            val message: String,
            override val error: String? = null,
        ) : State()

        /**
         * The browser has been handed to. Nothing is polled: the redirect comes
         * back through [onAuthorizationRedirect] when the user is done.
         */
        data class AwaitingRedirect(
            override val url: String,
            override val backendType: BackendType = BackendType.READECK,
            val browserUrl: String? = null,
            override val error: String? = null,
        ) : State()

        data class DeviceFlow(
            override val url: String,
            val session: com.neverreader.backend.readeck.DeviceSession,
            override val backendType: BackendType = BackendType.READECK,
            override val error: String? = null,
        ) : State()
    }

    sealed class Event {
        data object Success : Event()
        data class OpenBrowser(val url: String) : Event()
    }

    companion object {
        const val CODE_PARAM = "code"
        const val STATE_PARAM = "state"
        const val ERROR_PARAM = "error"
        const val ERROR_DESCRIPTION_PARAM = "error_description"

        private const val KEY_SERVER_URL = "auth.pendingServerUrl"
        private const val KEY_VERIFIER = "auth.pendingVerifier"
        private const val KEY_STATE = "auth.pendingState"

        /**
         * Where the user finds the credentials, so the setup screen can point at it
         * rather than leaving two bare fields unexplained.
         */
        const val WALLABAG_CLIENT_HELP =
            "From your Wallabag: Settings -> API clients. The client_id it lists " +
                "looks like 1_AbCdEf, and the secret is beside it."
    }
}
