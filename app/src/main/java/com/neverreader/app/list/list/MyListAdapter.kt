package com.neverreader.app.list.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.neverreader.app.R
import com.neverreader.app.databinding.ViewListItemRowBinding
import com.neverreader.app.list.ListItemUiState
import com.neverreader.app.list.MyListViewModel
import com.neverreader.backend.model.Bookmark

class MyListAdapter(
    private val viewModel: MyListViewModel,
) : PagingDataAdapter<ListItemUiState, MyListAdapter.ItemRowViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemRowViewHolder =
        ItemRowViewHolder(
            ViewListItemRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ItemRowViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item)
    }

    inner class ItemRowViewHolder(
        private val binding: ViewListItemRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(state: ListItemUiState) = with(binding) {
            title.text = state.title
            domain.text = state.bookmark.url
            excerpt.text = state.excerpt
            excerpt.visibility = if (state.excerpt.isBlank()) View.GONE else View.VISIBLE
            timeEstimate.text = state.bookmark.readingTimeMinutes.let { "$it min" }
            favorite.setImageResource(
                if (state.favorite) com.neverreader.ui.R.drawable.ic_nr_favorite_solid
                else com.neverreader.ui.R.drawable.ic_nr_favorite_line
            )
            setThumbnail(state, thumbnail)
            root.setOnClickListener { viewModel.onItemClicked(state.bookmark) }
            favorite.setOnClickListener { viewModel.toggleFavorite(state.bookmark) }
            overflow.setOnClickListener { viewModel.archive(state.bookmark) }
        }

        private fun setThumbnail(state: ListItemUiState, thumbnailView: ImageView) {
            val url = state.imageUrl
            if (url.isNullOrBlank()) {
                thumbnailView.visibility = View.GONE
            } else {
                thumbnailView.visibility = View.VISIBLE
                thumbnailView.setImageResource(com.neverreader.ui.R.drawable.ic_nr_archive_solid)
            }
        }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ListItemUiState>() {
            override fun areItemsTheSame(
                oldItem: ListItemUiState,
                newItem: ListItemUiState,
            ): Boolean = oldItem.bookmark.id == newItem.bookmark.id

            override fun areContentsTheSame(
                oldItem: ListItemUiState,
                newItem: ListItemUiState,
            ): Boolean = oldItem == newItem
        }
    }
}
