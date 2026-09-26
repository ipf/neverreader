package com.neverreader.ui.view.menu

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neverreader.ui.R
import com.neverreader.ui.view.themed.ThemedRecyclerView

/**
 * A simple options list which takes a String[] of options and a list item click listener, intended for use in DialogViews.
 */
class SimpleOptionsRecyclerView : ThemedRecyclerView {
    interface OnItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }

    private var listener: OnItemClickListener? = null

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context) : super(context!!) {
        init()
    }

    private fun init() {
        setBackgroundResource(R.drawable.cl_nr_bg)
        setLayoutManager(LinearLayoutManager(context))
        // hide the top divider
        setPadding(
            0,
            -getContext().getResources().getDimension(R.dimen.nr_thin_divider_height).toInt(),
            0,
            0
        )
    }

    private inner class Adapter(private val data: Array<String?>) :
        RecyclerView.Adapter<Adapter.ViewHolder>() {
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val textView: TextView

            init {
                textView = v.findViewById<TextView>(android.R.id.text1)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.view_nr_simple_list_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.setText(data[position])
            holder.textView.setOnClickListener(OnClickListener { v: View? ->
                if (listener != null) {
                    listener!!.onItemClick(holder.textView, position)
                }
            })
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }
}
