package com.neverreader.ui.view.progress.skeleton

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neverreader.ui.view.progress.skeleton.row.SkeletonItemRow
import com.neverreader.ui.view.themed.ThemedRecyclerView

class SkeletonList : ThemedRecyclerView {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    fun init() {
        setHasFixedSize(true)
        setClipToPadding(false)

        val manager: LayoutManager =
            LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false)
        setLayoutManager(manager)

        setAdapter(Adapter())
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v: View = SkeletonItemRow(getContext())
            v.setLayoutParams(
                ViewGroup.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
            )

            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: Adapter.ViewHolder, position: Int) {
            // Nothing to do here.
        }

        override fun getItemCount(): Int {
            return LIST_SIZE
        }
    }

    companion object {
        private const val LIST_SIZE = 20
    }
}
