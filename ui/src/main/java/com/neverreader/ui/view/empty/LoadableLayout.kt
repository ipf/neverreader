package com.neverreader.ui.view.empty

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.neverreader.ui.R
import com.neverreader.ui.util.NeverReaderUIViewUtil
import com.neverreader.ui.view.progress.RainbowProgressCircleView
import com.neverreader.ui.view.visualmargin.VisualMarginConstraintLayout

/**
 * A view that can display a loading, loaded or error state.
 */
class LoadableLayout : VisualMarginConstraintLayout {
    private val binder = Binder(this)
    private var empty: EmptyView? = null

    private var defaultProgressView: RainbowProgressCircleView? = null
    private var currentProgressView: View? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_loadable, this, true)
        defaultProgressView = findViewById<RainbowProgressCircleView>(R.id.progress)
        currentProgressView = defaultProgressView
        empty = findViewById<EmptyView>(R.id.empty)
    }

    fun bind(): Binder {
        return binder
    }

    class Binder constructor(private val view: LoadableLayout) {
        fun clear(): Binder {
            view.empty!!.bind().clear()
            customProgressIndicator(view.defaultProgressView)
            showProgressIndeterminate()
            return this
        }

        fun showEmptyOrError(): EmptyView.Binder? {
            view.empty!!.setVisibility(VISIBLE)
            view.currentProgressView!!.setVisibility(GONE)
            return view.empty!!.bind()
        }

        fun showProgressIndeterminate(): Binder {
            view.defaultProgressView!!.setProgressIndeterminate(true)
            showProgress()
            return this
        }

        // TODO custom progress views don't currently support determinate progress
        fun showProgress(progress: Float): Binder {
            view.defaultProgressView!!.setProgress(progress)
            showProgress()
            return this
        }

        /**
         * Adds a custom View as the progress indicator.
         *
         * @param v The custom progress indicator View
         */
        fun customProgressIndicator(v: View?): Binder {
            if (v === view.currentProgressView) {
                return this
            }
            if (v == null) {
                if (view.currentProgressView !== view.defaultProgressView) {
                    view.defaultProgressView!!.setVisibility(view.currentProgressView!!.getVisibility())
                    NeverReaderUIViewUtil.replaceView(
                        view.currentProgressView!!,
                        view.defaultProgressView!!
                    )
                    constraintParamsWrapContent()
                }
            } else {
                v.setVisibility(view.currentProgressView!!.getVisibility())
                NeverReaderUIViewUtil.replaceView(view.currentProgressView!!, v)
                view.currentProgressView = v
                constraintParamsFillWidth()
            }
            return this
        }

        private fun showProgress() {
            view.empty!!.setVisibility(GONE)
            view.currentProgressView!!.setVisibility(VISIBLE)
        }

        // This is a hack for fixing constraintlayout params.
        // The default progress circle wraps its content and those params get copied in
        // replaceView.  Here we make the custom view match the parent size with width / height = 0.
        private fun constraintParamsFillWidth() {
            view.currentProgressView!!.getLayoutParams().width = 0
            view.currentProgressView!!.getLayoutParams().height = 0
        }

        private fun constraintParamsWrapContent() {
            view.currentProgressView!!.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT
            view.currentProgressView!!.getLayoutParams().height =
                ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }
}
