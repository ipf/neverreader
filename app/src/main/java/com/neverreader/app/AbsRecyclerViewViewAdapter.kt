package com.neverreader.app

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView

/**
 * A [RecyclerView.Adapter] that also supports adding arbitrary Views, useful for adding headers, footers, buttons, etc.
 *
 * To use, wrap your data types in the [Row] class and implement onCreateViewHolder(@NonNull ViewGroup parent, int viewType),
 * being sure to call the superclass implementation in the event that your own ViewHolders are not applicable.
 */
abstract class AbsRecyclerViewViewAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val viewMap = SparseArray<View?>()
    private val data: MutableList<Row?> = ArrayList<Row?>()

    abstract class Row {
        abstract fun getViewType(): Int
    }

    private class ViewRow(val view: View) : Row() {
        override fun getViewType(): Int {
            return getViewViewType(view.getId()) // ensures this ViewHolder is not recycled
        }
    }

    abstract inner class ViewHolder<T : Row?>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(row: T?)
    }

    private inner class ViewHolderView(view: View) : ViewHolder<ViewRow?>(view) {
        override fun bind(row: ViewRow?) {
            // nothing to do here
        }
    }

    @CallSuper
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<*> {
        return ViewHolderView(viewMap.get(viewType)!!)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder<Row?>).bind(data.get(position))
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return data.get(position)!!.getViewType()
    }

    fun addItem(index: Int, row: Row?) {
        data.add(index, row)
        notifyItemInserted(index)
    }

    fun addItems(index: Int, items: MutableList<Row?>) {
        data.addAll(index, items)
        notifyItemRangeInserted(index, items.size)
    }

    fun removeItems(positionStart: Int, itemCount: Int) {
        data.subList(positionStart, positionStart + itemCount).clear()
        notifyItemRangeRemoved(positionStart, itemCount)
    }

    fun addView(v: View) {
        addView(data.size, v)
    }

    fun addView(index: Int, v: View) {
        if (v.getId() == -1) {
            v.setId(View.generateViewId())
        }
        viewMap.put(getViewViewType(v.getId()), v)
        addItem(index, ViewRow(v))
    }

    companion object {
        /**
         * Generates a unique view type for a View added to the adapter.  This value is based on a negative Integer.MAX_VALUE
         * to avoid colliding with the generally used method of incrementing view types for custom ViewHolders.
         */
        private fun getViewViewType(viewId: Int): Int {
            return -Int.MAX_VALUE + viewId
        }
    }
}
