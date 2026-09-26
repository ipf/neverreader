package com.neverreader.ui.view.menu

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.view.themed.ThemedRecyclerView

class ThemedPopupMenu(context: Context, vararg sections: Section) {
    private val view: RecyclerView

    /** CharSequences as headers, MenuItem as options.  */
    private val rows: MutableList<Any?> = ArrayList<Any?>()
    private val types: MutableMap<MenuItem?, MenuType?> = HashMap<MenuItem?, MenuType?>()
    private val selected: MutableSet<MenuItem?> = HashSet<MenuItem?>()
    private var window: PopupWindow? = null

    init {
        for (section in sections) {
            if (section.label != null) {
                rows.add(section.label)
            }
            for (option in section.options) {
                rows.add(option)
                types.put(option, section.type)
            }
            if (section.selectedPosition >= 0) {
                selected.add(section.options.get(section.selectedPosition))
            }
        }

        view = ThemedRecyclerView(context)
        view.setBackgroundResource(R.drawable.cl_nr_popup_bg)
        view.setLayoutManager(LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false))
        view.setAdapter(object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemViewType(position: Int): Int {
                if (rows[position] is CharSequence) {
                    return VIEW_TYPE_HEADER
                } else {
                    val option = rows[position] as MenuItem?
                    when (types[option]) {
                        MenuType.RADIO -> return VIEW_TYPE_RADIO_OPTION
                        MenuType.ACTIONS -> return VIEW_TYPE_ACTION_OPTION
                        else -> return VIEW_TYPE_HEADER
                    }
                }
                throw RuntimeException("unknown type at position $position")
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                when (viewType) {
                    VIEW_TYPE_HEADER -> return HeaderHolder(context!!)
                    VIEW_TYPE_RADIO_OPTION -> return RadioOptionHolder(context!!)
                    else -> return ActionOptionHolder(context!!)
                }
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                when (holder) {
                    is HeaderHolder -> {
                        holder.bind(rows[position] as CharSequence?)
                    }

                    is RadioOptionHolder -> {
                        val option = rows[position] as MenuItem
                        holder.bind(option, selected.contains(option))
                    }

                    is ActionOptionHolder -> {
                        val option = rows[position] as MenuItem
                        holder.bind(option)
                    }
                }
            }

            override fun getItemCount(): Int {
                return rows.size
            }
        })

        view.minimumWidth = dpToPxInt(context, 200f)
    }

    fun show(anchor: View) {
        dismiss()
        window = PopupWindow(view.context)
        window!!.setBackgroundDrawable(
            ContextCompat.getDrawable(
                anchor.context,
                R.drawable.nr_popup_bg
            )
        )
        window!!.isFocusable = true
        window!!.contentView = view
        window!!.width = WindowManager.LayoutParams.WRAP_CONTENT
        window!!.height = WindowManager.LayoutParams.WRAP_CONTENT
        window!!.showAsDropDown(anchor, -anchor.width, -anchor.height)
    }

    private fun dismiss() {
        if (window != null) {
            window!!.dismiss()
            window = null
        }
    }

    private class HeaderHolder(context: Context) :
        RecyclerView.ViewHolder(SectionHeaderView(context)) {
        init {
            itemView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        fun bind(stringResId: Int) {
            (itemView as SectionHeaderView).bind().clear()
                .showBottomDivider(false)
                .label(stringResId)
        }

        fun bind(label: CharSequence?) {
            (itemView as SectionHeaderView).bind().clear()
                .showBottomDivider(false)
                .label(label)
        }
    }

    private inner class RadioOptionHolder(context: Context?) :
        RecyclerView.ViewHolder(RadioOptionRowView(context)) {
        init {
            itemView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        fun bind(option: MenuItem, selected: Boolean) {
            val view = (itemView as RadioOptionRowView)
            view.setLabel(option.label)
            view.setOnClickListener(View.OnClickListener { v: View? ->
                dismiss()
                option.onClick(v)
            })
            view.isChecked = selected
            view.isEnabled = option.isEnabled
        }
    }

    private inner class ActionOptionHolder(context: Context?) :
        RecyclerView.ViewHolder(OptionRowView(context)) {
        init {
            itemView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        fun bind(option: MenuItem) {
            val view = (itemView as OptionRowView)
            view.uiEntityIdentifier = option.uiEntityIdentifier
            view.setLabel(option.label)
            view.setIcon(option.icon)
            view.setOnClickListener(View.OnClickListener { v: View? ->
                dismiss()
                option.onClick(v)
            })
            view.isEnabled = option.isEnabled
        }
    }

    enum class MenuType {
        ACTIONS,
        RADIO
    }

    class Section private constructor(
        val type: MenuType?,
        val label: CharSequence?,
        val selectedPosition: Int,
        val options: MutableList<MenuItem?>
    ) {
        companion object {
            fun actions(label: CharSequence?, options: MutableList<MenuItem?>): Section {
                return Section(MenuType.ACTIONS, label, -1, options)
            }

            fun radio(
                label: CharSequence?,
                selectedPosition: Int,
                options: MutableList<MenuItem?>
            ): Section {
                return Section(MenuType.RADIO, label, selectedPosition, options)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_RADIO_OPTION = 1
        private const val VIEW_TYPE_ACTION_OPTION = 2
    }
}
