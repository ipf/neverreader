package com.neverreader.ui.view.info

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.neverreader.ui.R
import com.neverreader.ui.view.themed.ThemedConstraintLayout

/**
 * A View which displays a ViewPager along with a PageIndicatorView (page dots) that tracks page changes.
 */
class PageIndicatedViewPager : ThemedConstraintLayout {
    /**
     * An OnPageChangeListener which tracks page changes in order to update the indicator dots.
     * If the OnPageChangeListener list gets cleared, this gets re-added internally, as it's
     * always needed.
     */
    var indicatorListener: OnPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            indicators!!.bind().currentIndex(position)
        }
    }

    private val binder: Binder = Binder()

    private var pager: ViewPager2? = null
    private var indicators: PageIndicatorView? = null

    private var adapter: RecyclerView.Adapter<*>? = null

    private var onPageChangeListeners: MutableList<OnPageChangeCallback>? = null

    constructor(context: Context) : super(context!!) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_indicator_viewpager, this, true)
        pager = findViewById<ViewPager2>(R.id.pager)
        indicators = findViewById<PageIndicatorView>(R.id.indicators)
        onPageChangeListeners = ArrayList<OnPageChangeCallback>()
        binder.addOnPageChangeListener(indicatorListener)
        pager!!.getChildAt(0)
            .setOverScrollMode(OVER_SCROLL_NEVER) // sets the overscroll mode of the internal RecyclerView of ViewPager2
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return pager!!.getChildAt(0)
            .onTouchEvent(event) // Let the internal RecyclerView handle, so that the indicator area is also swipable.
    }

    val currentPage: Int
        get() = pager!!.getCurrentItem()

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            adapter(null)
            indicators!!.bind().clear()
            clearOnPageChangeListeners()
            return this
        }

        fun adapter(value: RecyclerView.Adapter<*>?): Binder {
            adapter = value
            pager!!.setAdapter(adapter)

            if (adapter != null) {
                indicators!!.bind().pageCount(adapter!!.getItemCount())
            }

            indicators!!.setVisibility(if (adapter == null || adapter!!.getItemCount() < 2) GONE else VISIBLE)

            return this
        }

        /**
         * Move to the previous page.
         * @return true if advanced, false if already at first page or there are no pages
         */
        fun previousPage(): Boolean {
            if (adapter!!.getItemCount() > 0 && pager!!.getCurrentItem() > 0) {
                pager!!.setCurrentItem(pager!!.getCurrentItem() - 1)
                return true
            } else {
                return false
            }
        }

        /**
         * Move to the next page.
         * @return true if advanced, false if already at final page
         */
        fun nextPage(): Boolean {
            if (pager!!.getCurrentItem() < adapter!!.getItemCount()) {
                pager!!.setCurrentItem(pager!!.getCurrentItem() + 1)
                return true
            } else {
                return false
            }
        }

        fun setPage(index: Int) {
            pager!!.setCurrentItem(index)
        }

        fun addOnPageChangeListener(listener: OnPageChangeCallback?): Binder {
            if (listener != null) {
                pager!!.registerOnPageChangeCallback(listener)
            }
            if (listener != null) {
                onPageChangeListeners!!.add(listener)
            }
            return this
        }

        fun removeOnPageChangeListener(listener: OnPageChangeCallback?): Binder {
            if (listener != null) {
                pager!!.unregisterOnPageChangeCallback(listener)
            }
            onPageChangeListeners!!.remove(listener)
            return this
        }

        fun clearOnPageChangeListeners() {
            for (callback in onPageChangeListeners!!) {
                pager!!.unregisterOnPageChangeCallback(callback)
            }
            onPageChangeListeners!!.clear()
            // the dot listener is always needed, so re-add it
            addOnPageChangeListener(indicatorListener)
        }
    }
}
