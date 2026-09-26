package com.neverreader.ui.view.info

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.neverreader.ui.R
import com.neverreader.ui.view.button.BoxButton
import com.neverreader.ui.view.themed.ThemedConstraintLayout

/**
 * A fullscreen view which takes lists of [InfoPage] data and displays them.  An InfoPage consists of an image, title text, subtext,
 * and optionally a button text, button listener, and link text and listener.
 *
 *
 * Design: https://www.figma.com/file/Qqwh8xKl4Gy4YMv6mzw2gCO9/CLEAN?node-id=640%3A8213
 */
class InfoPagingView : ThemedConstraintLayout {
    abstract class InfoAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        abstract val data: MutableList<InfoPage>?
    }

    private val binder: Binder = Binder()

    private var pages: MutableList<InfoPage>? = ArrayList<InfoPage>()

    private var header: ImageView? = null
    private var pager: PageIndicatedViewPager? = null
    private var actionButton: BoxButton? = null
    private var linkText: TextView? = null

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

    constructor(context: Context) : super(context!!) {
        init()
    }

    private fun init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_info_paging_view, this, true)
        setBackgroundResource(R.drawable.cl_nr_bg)
        header = findViewById<ImageView>(R.id.header)
        pager = findViewById<PageIndicatedViewPager>(R.id.viewPager)
        actionButton = findViewById<BoxButton>(R.id.actionButton)
        linkText = findViewById<TextView>(R.id.actionLinkText)


        // This captures any touches to the content area that didn't get used by buttons or paging yet and passes them to the pager so swiping can happen from anywhere in this area.
        // This gives the view the feeling that the whole view is part of the pager
        // This is needed in addition to the onTouchEvent override below, otherwise the scroll view eats all events. If needed the scroll view will intercept these for vertical scrolling.
        findViewById<View>(R.id.info_page_content).setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            pager!!.onTouchEvent(
                event
            )
        })
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // This captures any unhandled touches above and below the centered scrollable content and passes them to the pager for swiping.
        // This gives the view the feeling that the whole view is part of the pager
        return super.onTouchEvent(event) || pager!!.onTouchEvent(event)
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            adapter(null)
            header(null)
            pager!!.bind().clear()
            return this
        }

        /**
         * Change/set the adapter. Any listeners previously added via [.addOnPageChangeListener] will be cleared.
         */
        fun adapter(adapter: InfoAdapter?): Binder {
            if (adapter != null) {
                pages = adapter.data

                pager!!.bind()
                    .clearOnPageChangeListeners() // clear any previous adapter's OnPageChangeListener

                pager!!.bind().adapter(adapter)
                    .addOnPageChangeListener(object : OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            val page = pages!!.get(position)

                            // post to ensure we're running on the UI thread
                            actionButton!!.post(Runnable {
                                // check for top blue button
                                if (page.buttonListener == null) {
                                    actionButton!!.setVisibility(GONE)
                                    actionButton!!.setOnClickListener(null)
                                } else {
                                    actionButton!!.setVisibility(VISIBLE)
                                    actionButton!!.setOnClickListener(page.buttonListener)
                                    actionButton!!.setText(page.buttonText)
                                }

                                // check for link text
                                if (page.linkButtonListener == null) {
                                    // Always keep this view visible if the top button is, so it holds a consistent height.
                                    linkText!!.setVisibility(if (actionButton!!.getVisibility() == GONE) GONE else VISIBLE)
                                    // The text is just emptied rather than using View.INVISIBLE because View.INVISIBLE causes
                                    // its ripple animation to flicker or on some Android versions, stick around after the page changes.
                                    linkText!!.setText("")
                                    linkText!!.setOnClickListener(null)
                                    linkText!!.setClickable(false)
                                } else {
                                    linkText!!.setVisibility(VISIBLE)
                                    linkText!!.setOnClickListener(page.linkButtonListener)
                                    linkText!!.setText(page.linkButtonText)
                                }
                            })
                        }
                    }).setPage(0) // init to first page
            } else {
                pages = null
                pager!!.bind().adapter(null)
            }

            return this
        }

        fun header(@DrawableRes drawable: Int): Binder {
            return header(getResources().getDrawable(drawable))
        }

        fun header(drawable: Drawable?): Binder {
            header!!.setImageDrawable(drawable)
            header!!.setVisibility(if (drawable != null) VISIBLE else GONE)
            return this
        }

        /**
         * Move to the previous page.
         * @return true if advanced, false if already at first page
         */
        fun previousPage(): Boolean {
            return pager!!.bind().previousPage()
        }

        /**
         * Move to the next page.
         * @return true if advanced, false if already at final page
         */
        fun nextPage(): Boolean {
            return pager!!.bind().nextPage()
        }

        /**
         * Adds a page change listener. Invoke this after [.adapter].
         */
        fun addOnPageChangeListener(listener: OnPageChangeCallback?): Binder {
            pager!!.bind().addOnPageChangeListener(listener)
            return this
        }

        fun removeOnPageChangeListener(listener: OnPageChangeCallback?): Binder {
            pager!!.bind().removeOnPageChangeListener(listener)
            return this
        }
    }
}
