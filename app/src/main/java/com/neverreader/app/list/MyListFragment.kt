package com.neverreader.app.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.neverreader.app.R
import com.neverreader.app.databinding.FragMyListBinding
import com.neverreader.app.list.add.AddUrlBottomSheetFragment
import com.neverreader.app.list.list.ListManager
import com.neverreader.app.list.list.MyListAdapter
import com.neverreader.sdk.util.AbsNeverReaderFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The saves list screen: unread / favorites / archive with filtering.
 */
@AndroidEntryPoint
class MyListFragment : AbsNeverReaderFragment() {

    private var _binding: FragMyListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyListViewModel by viewModels()
    private val adapter by lazy { MyListAdapter(viewModel) }

    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragMyListBinding.inflate(inflater!!,  container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.listRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.listRecyclerView.adapter = adapter

        binding.addButton.setOnClickListener { showAddUrl() }
        binding.refreshLayout.setOnRefreshListener { sync() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pagedBookmarks.collectLatest {
                        adapter.submitData(it)
                    }
                }
                launch {
                    viewModel.navigationEvents.collect { event ->
                        when (event) {
                            is MyListNavigationEvent.ShowAddUrl -> showAddUrl()
                            is MyListNavigationEvent.OpenReader -> {
                                // handled by the nav graph
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sync() {
        // The repository syncs in the background; the list updates automatically.
        Snackbar.make(binding.root, R.string.ac_ok, Snackbar.LENGTH_SHORT).show()
        binding.refreshLayout.isRefreshing = false
    }

    private fun showAddUrl() {
        AddUrlBottomSheetFragment().show(parentFragmentManager, AddUrlBottomSheetFragment::class.java.name)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
