package com.neverreader.app.list.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neverreader.app.R
import com.neverreader.ui.view.button.BoxButton
import com.neverreader.ui.theme.AppTheme

/**
 * The "save a URL" sheet.
 *
 * A ModalBottomSheet rather than a BottomSheetDialogFragment: navigation
 * destinations are composables now, and reaching for a FragmentManager from one
 * to show a dialog would be the last place the two worlds touched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUrlSheet(
    onDismiss: () -> Unit,
    viewModel: AddUrlBottomSheetViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // The ViewModel holds these as Compose state, so they are read directly
    // rather than collected.
    val text = viewModel.textFieldValue
    val isError = viewModel.textFieldIsError

    LaunchedEffect(Unit) {
        viewModel.onViewShown()
        viewModel.navigationEvents.collect { event ->
            when (event) {
                AddUrlBottomSheetViewModel.NavigationEvent.Close -> onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colors.background,
    ) {
        AddUrlSheetContent(
            textFieldValue = text,
            isError = isError,
            onTextFieldValueChange = viewModel::onTextFieldValueChange,
            onSaveButtonClick = viewModel::onSaveButtonClick,
        )
    }
}

@Composable
private fun AddUrlSheetContent(
    textFieldValue: String,
    onTextFieldValueChange: (String) -> Unit,
    onSaveButtonClick: () -> Unit,
    isError: Boolean,
) {
    Column(
        Modifier.padding(
            horizontal = AppTheme.dimensions.sideGrid,
            vertical = dimensionResource(com.neverreader.ui.R.dimen.nr_space_md),
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.add_url_title),
            style = AppTheme.typography.h5,
        )
        Spacer(Modifier.height(dimensionResource(com.neverreader.ui.R.dimen.nr_space_lg)))
        AddUrlTextField(textFieldValue, onTextFieldValueChange, isError)
        Spacer(Modifier.height(dimensionResource(com.neverreader.ui.R.dimen.nr_space_md)))
        BoxButton(
            text = stringResource(R.string.mu_read_later),
            onClick = onSaveButtonClick,
            Modifier
                .fillMaxWidth()
                .height(48.dp),
        )
        Spacer(Modifier.height(dimensionResource(com.neverreader.ui.R.dimen.nr_space_md)))
    }
}

@Composable
private fun AddUrlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
) {
    val focusRequester = androidx.compose.runtime.remember { FocusRequester() }
    val colors = AppTheme.colors
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(stringResource(R.string.add_url_hint)) },
        isError = isError,
        supportingText = if (isError) {
            { Text(stringResource(R.string.add_url_error)) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        singleLine = true,
        // Material's defaults here are a purple that appears nowhere else in the
        // app; the theme owns these, as it does for the server-URL field.
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = colors.teal1,
            unfocusedIndicatorColor = colors.divider,
            focusedTextColor = colors.grey1,
            unfocusedTextColor = colors.grey1,
            cursorColor = colors.teal1,
            focusedPlaceholderColor = colors.grey4,
            unfocusedPlaceholderColor = colors.grey4,
        ),
    )
    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}
