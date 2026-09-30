package com.neverreader.app.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neverreader.app.R
import com.neverreader.backend.model.BackendType
import com.neverreader.ui.compose.AppBar
import com.neverreader.ui.compose.FilterChips
import com.neverreader.ui.theme.AppTheme

/**
 * The server setup flow: pick a backend (Readeck or Wallabag), enter the server
 * URL, then sign in.
 *
 * Readeck opens the browser and is redirected straight back once the user
 * approves, so there is nothing to do afterwards. The device-code flow is kept
 * as a secondary action for a device with no browser to redirect.
 */
@Composable
fun AuthenticationScreen(
    viewModel: AuthenticationViewModel = hiltViewModel(),
    onOpenUrl: (String) -> Unit,
) {
    val current by viewModel.state.collectAsStateWithLifecycle()
    val state = current

    // The device flow is only useful if the user can act on the code, so open the
    // browser as soon as a session arrives rather than waiting for a tap.
    LaunchedEffect(state) {
        if (state is AuthenticationViewModel.State.DeviceFlow) {
            onOpenUrl(state.session.verificationUriComplete ?: state.session.verificationUri)
        }
    }

    // One root layout. A bare ComposeView positions every top-level child at
    // (0,0), so emitting the app bar, the backend chips and the form as siblings
    // made them draw on top of each other - which is what buried the Authorize
    // button and made the top bar look overlaid.
    Column(Modifier.fillMaxSize()) {
        AppBar(title = { Text(stringResource(R.string.auth_title)) })

        FilterChips(
            tabs = BackendType.entries,
            selected = state.backendType,
            onSelect = viewModel::onBackendTypeChange,
            label = { type ->
                stringResource(
                    when (type) {
                        BackendType.READECK -> R.string.auth_backend_readeck
                        BackendType.WALLABAG -> R.string.auth_backend_wallabag
                    }
                )
            },
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = AppTheme.dimensions.sideGrid),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall),
        ) {
            Text(
                text = stringResource(R.string.auth_subtitle),
                style = AppTheme.typography.p4,
                color = AppTheme.colors.textSecondary,
            )

            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::onServerUrlChange,
                label = { Text(stringResource(R.string.auth_server_url_hint)) },
                singleLine = true,
                isError = !state.error.isNullOrBlank(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                // Material's default focus/cursor colour is a purple that appears
                // nowhere else in the app; the theme owns these.
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.teal1,
                    unfocusedBorderColor = AppTheme.colors.grey3,
                    cursorColor = AppTheme.colors.teal1,
                    focusedLabelColor = AppTheme.colors.teal1,
                    unfocusedLabelColor = AppTheme.colors.grey3,
                    focusedTextColor = AppTheme.colors.grey1,
                    unfocusedTextColor = AppTheme.colors.grey1,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.backendType == BackendType.WALLABAG) {
                WallabagCredentials(
                    enabled = state !is AuthenticationViewModel.State.Authorizing,
                    onSubmit = viewModel::loginWallabag,
                )
            }

            when (state) {
                is AuthenticationViewModel.State.Authorizing -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(AppTheme.dimensions.spaceSmall))
                    Text(state.message, style = AppTheme.typography.p4)
                }

                // The browser has the user; the redirect ends the flow, so this
                // only has to say so.
                is AuthenticationViewModel.State.AwaitingRedirect -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(AppTheme.dimensions.spaceSmall))
                    Text(
                        text = stringResource(R.string.auth_waiting_for_browser),
                        style = AppTheme.typography.p4,
                    )
                }

                is AuthenticationViewModel.State.DeviceFlow -> {
                    Text(
                        text = stringResource(R.string.auth_enter_code_in_browser),
                        style = AppTheme.typography.p4,
                        color = AppTheme.colors.textSecondary,
                    )
                    Text(
                        text = state.session.userCode,
                        style = AppTheme.typography.h5,
                    )
                }

                is AuthenticationViewModel.State.EnterServerUrl -> {
                    if (state.backendType == BackendType.READECK) {
                        PrimaryButton(
                            text = stringResource(R.string.auth_authorize),
                            onClick = viewModel::startReadeckAuthorization,
                        )
                        Text(
                            text = stringResource(R.string.auth_use_code_instead),
                            style = AppTheme.typography.p4,
                            color = AppTheme.colors.teal1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = viewModel::startReadeckDeviceFlow)
                                .padding(vertical = AppTheme.dimensions.spaceSmall),
                        )
                    }
                }
            }

            state.error?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = AppTheme.typography.p4,
                    color = AppTheme.colors.coral2,
                )
            }
        }
    }
}

/**
 * Wallabag uses the OAuth2 password grant, so it needs credentials the Readeck
 * device flow does not.
 */
@Composable
private fun WallabagCredentials(
    enabled: Boolean,
    onSubmit: (String, String, String, String) -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf("") }
    var clientSecret by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall)) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.auth_username)) },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.auth_password)) },
            singleLine = true,
            enabled = enabled,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it },
            label = { Text(stringResource(R.string.auth_client_id)) },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = clientSecret,
            onValueChange = { clientSecret = it },
            label = { Text(stringResource(R.string.auth_client_secret)) },
            singleLine = true,
            enabled = enabled,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        // Four blank fields with no explanation is a bad time to be told the
        // password grant needs an OAuth client.
        Text(
            text = stringResource(R.string.auth_client_help),
            style = AppTheme.typography.p4,
            color = AppTheme.colors.textSecondary,
        )
        PrimaryButton(
            text = stringResource(R.string.auth_authorize),
            enabled = enabled &&
                username.isNotBlank() && password.isNotBlank() &&
                clientId.isNotBlank() && clientSecret.isNotBlank(),
            onClick = { onSubmit(username, password, clientId, clientSecret) },
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.primary,
            contentColor = AppTheme.colors.onPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.dimensions.spaceSmall),
    ) {
        Text(text, textAlign = TextAlign.Center)
    }
}
