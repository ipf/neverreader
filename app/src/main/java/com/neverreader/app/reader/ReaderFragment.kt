package com.neverreader.app.reader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import com.neverreader.app.databinding.FragmentReaderBinding
import com.neverreader.sdk.util.AbsNeverReaderFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The article reader: renders the backend's article HTML in a WebView.
 */
@AndroidEntryPoint
class ReaderFragment : AbsNeverReaderFragment() {

    private var _binding: FragmentReaderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReaderViewModel by viewModels()

    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentReaderBinding.inflate(inflater!!,  container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.webView.settings.javaScriptEnabled = false
        binding.webView.settings.textZoom = 100
        binding.webView.webViewClient = object : android.webkit.WebViewClient() {}

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is ReaderViewModel.State.Content -> {
                        binding.webView.loadDataWithBaseURL(
                            state.url,
                            state.html,
                            "text/html",
                            "utf-8",
                            null,
                        )
                    }
                    is ReaderViewModel.State.Error -> {
                        binding.webView.loadData(
                            "<html><body><h3>Could not load this article.</h3></body></html>",
                            "text/html",
                            "utf-8",
                        )
                    }
                    is ReaderViewModel.State.Loading -> Unit
                }
            }
        }

        arguments?.getString("url")?.let { viewModel.load(it) }
    }

    override fun onDestroyView() {
        binding.webView.destroy()
        super.onDestroyView()
        _binding = null
    }
}
