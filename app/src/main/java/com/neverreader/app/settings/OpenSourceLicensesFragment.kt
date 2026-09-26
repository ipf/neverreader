package com.neverreader.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import com.neverreader.app.R
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.ui.compose.AppBar
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon
import com.neverreader.ui.view.themed.AppTheme

@AndroidEntryPoint
class OpenSourceLicensesFragment : AbsNeverReaderFragment() {
    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        AppTheme {
            Column {
                AppBar(
                    navigationIcon = {
                        AppIconButton(onClick = { findNavController().navigateUp() }) {
                            UpIcon()
                        }
                    },
                    title = { Text(stringResource(R.string.setting_oss)) },
                )
                LibrariesContainer(
                    Modifier.fillMaxSize(),
                    showVersion = false,
                )
            }
        }
    }
}
