package com.neverreader.app.list.list

import androidx.recyclerview.widget.RecyclerView
import com.neverreader.util.android.view.LayoutManagerUtil
import com.neverreader.app.list.MyListViewModel

class MyListPagingScrollListener(
    private val adapter: MyListAdapter,
    private val viewModel: MyListViewModel,
) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        val lastVisiblePosition = LayoutManagerUtil.findLastVisibleItemPosition(recyclerView)
        if (lastVisiblePosition >= adapter.itemCount - PAGING_ITEM_THRESHOLD) {
            viewModel.onScrolledNearBottom()
        }
    }

    companion object {
        const val PAGING_ITEM_THRESHOLD = 20
    }
}