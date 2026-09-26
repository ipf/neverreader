package com.neverreader.app.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.neverreader.app.R
import androidx.core.widget.doAfterTextChanged
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import com.neverreader.app.databinding.FragmentAuthenticationBinding
import com.neverreader.app.MainActivity
import com.neverreader.backend.model.BackendType
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.sdk.util.AbsNeverReaderActivity
import kotlinx.coroutines.launch

/**
 * The server setup flow: pick a backend (Readeck or Wallabag), enter the server URL,
 * then authorize via the Readeck device flow or the Wallabag password grant.
 */
@AndroidEntryPoint
class AuthenticationFragment : AbsNeverReaderFragment() {

    private var _binding: FragmentAuthenticationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentAuthenticationBinding.inflate(inflater!!,  container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backendTypeReadeck.setOnClickListener { viewModel.onBackendTypeChange(BackendType.READECK) }
        binding.backendTypeWallabag.setOnClickListener { viewModel.onBackendTypeChange(BackendType.WALLABAG) }
        binding.serverUrl.doAfterTextChanged { viewModel.onServerUrlChange(it?.toString().orEmpty()) }
        binding.authorize.setOnClickListener { viewModel.startReadeckDeviceFlow() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        // Only sync from state when it differs, otherwise every keystroke
                        // (state update -> setText) resets the cursor to position 0.
                        if (binding.serverUrl.text?.toString() != state.url) {
                            binding.serverUrl.setText(state.url)
                        }
                        binding.error.text = state.error ?: ""
                        when (state) {
                            is AuthenticationViewModel.State.EnterServerUrl -> {
                                binding.status.text = ""
                                binding.deviceCode.text = ""
                            }
                            is AuthenticationViewModel.State.Authorizing -> {
                                binding.status.text = state.message
                            }
                            is AuthenticationViewModel.State.DeviceFlow -> {
                                binding.status.text = getString(R.string.auth_enter_code_in_browser)
                                binding.deviceCode.text = state.session.userCode
                                openVerificationUrl(state.session.verificationUriComplete ?: state.session.verificationUri)
                            }
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is AuthenticationViewModel.Event.Success -> {
                                // Login done: go to the main screen.
                                (activity as? AbsNeverReaderActivity)?.let { activity ->
                                    activity.startActivity(Intent(activity, MainActivity::class.java))
                                    activity.finish()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openVerificationUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    fun onNewIntent(intent: Intent?) {
        // nothing to handle here yet
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
