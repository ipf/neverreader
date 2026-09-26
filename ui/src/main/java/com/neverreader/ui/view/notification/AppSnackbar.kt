package com.neverreader.ui.view.notification

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPx
import com.neverreader.ui.util.NestedColorStateList.get
import com.neverreader.ui.view.button.ButtonBoxDrawable
import com.neverreader.ui.view.button.IconButton
import com.neverreader.ui.view.themed.ThemedTextView
import com.neverreader.util.android.setTextOrHide

/**
 * A custom view for showing notifications.
 *
 *
 * Design:
 * https://www.figma.com/file/Qqwh8xKl4Gy4YMv6mzw2gCO9/CLEAN?node-id=0%3A1
 */
class AppSnackbar : CoordinatorLayout {
    enum class Type {
        // used for NeverReader themed notifications inside of the NeverReader app
        ERROR_DISMISSABLE,
        ERROR_EXCLAIM,
        DEFAULT_DISMISSABLE,
        DEFAULT,

        // used for NeverReader themed notifications outside of the NeverReader app, such as the add overlay
        DEFAULT_OUTSIDE,
        ERROR_EXCLAIM_OUTSIDE
    }

    fun interface OnDismissListener {
        fun onDismiss(reason: DismissReason?)
    }

    enum class DismissReason {
        USER, PROGRAMMATIC
    }

    private val binder: Binder = Binder()

    private var root: ConstraintLayout? = null
    private var icon: IconButton? = null
    private var title: TextView? = null
    private var message: TextView? = null
    private var onDismissListener: OnDismissListener? = null
    private var action: ThemedTextView? = null
    private var isDismissed = false

    /**
     * A global error reporter, which handles what to do when long clicking on error snackbars.
     */
    fun interface ErrorReporter {
        fun reportError(context: Context?, message: String?, t: Throwable?)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
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
        LayoutInflater.from(getContext()).inflate(R.layout.view_nr_snackbar, this, true)

        root = findViewById<ConstraintLayout>(R.id.snackbar)
        icon = findViewById<IconButton>(R.id.icon)
        title = findViewById<TextView>(R.id.title)
        message = findViewById<TextView>(R.id.message)
        action = findViewById<ThemedTextView>(R.id.actionButton)

        setMinimumHeight(getResources().getDimension(R.dimen.nr_snackbar_height).toInt())
        setClipToPadding(false)
    }

    private fun userDismissable(dismissable: Boolean) {
        val layoutParams = root!!.getLayoutParams() as LayoutParams
        if (dismissable) {
            val swipeDismissBehavior: AppSwipeDismissBehavior<*> = AppSwipeDismissBehavior<View>()
            swipeDismissBehavior.setSwipeDirection(AppSwipeDismissBehavior.SWIPE_DIRECTION_ANY)
            swipeDismissBehavior.setListener(object : AppSwipeDismissBehavior.OnDismissListener {
                override fun onDismiss(view: View?) {
                    dismissMessage(DismissReason.USER)
                }

                override fun onDragStateChanged(i: Int) {
                    //
                }
            })
            layoutParams.behavior = swipeDismissBehavior
        } else {
            layoutParams.behavior = null
        }
    }

    private fun displayMessage() {
        alpha = 0f
        setVisibility(VISIBLE)
        animate().alpha(1f).setDuration(FADE_ANIM_MS.toLong()).setInterpolator(
            DecelerateInterpolator()
        ).setListener(null)
    }

    private fun dismissMessage(reason: DismissReason?) {
        if (isDismissed) return

        isDismissed = true
        animate().alpha(0f).setDuration(FADE_ANIM_MS.toLong()).setInterpolator(
            DecelerateInterpolator()
        ).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                setVisibility(GONE)
                if (onDismissListener != null) {
                    onDismissListener!!.onDismiss(reason)
                }


                // Clear out the listener, because the view caches the animator.
                animate().setListener(null)
            }
        })
    }

    fun show() {
        binder.show()
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        private var error: Throwable? = null

        fun clear(): Binder {
            setVisibility(VISIBLE)
            isDismissed = false
            type(Type.DEFAULT_DISMISSABLE)
            title(null)
            message(null)
            singleLineMessage(false)
            onDismiss(null)
            onAction(0, null)
            error(null)
            return this
        }

        fun type(type: Type): Binder {
            // default to no error icon / long press function

            icon!!.setVisibility(GONE)
            root!!.setOnLongClickListener(null)
            root!!.setLongClickable(false)

            val background: Drawable?
            when (type) {
                Type.ERROR_DISMISSABLE -> {
                    background = ButtonBoxDrawable(getContext(), R.color.nr_themed_apricot_1, ButtonBoxDrawable.CornerStyle.ALL)
                    userDismissable(true)
                    textColor(R.color.nr_button_text)
                    actionColor(R.color.nr_button_text)
                    setupErrorView(R.color.nr_button_text)
                }

                Type.ERROR_EXCLAIM -> {
                    background = ButtonBoxDrawable(getContext(), R.color.nr_themed_apricot_1, ButtonBoxDrawable.CornerStyle.ALL)
                    userDismissable(false)
                    textColor(R.color.nr_button_text)
                    actionColor(R.color.nr_button_text)
                    setupErrorView(R.color.nr_button_text)
                }

                Type.DEFAULT_DISMISSABLE -> {
                    background = ButtonBoxDrawable(getContext(), R.color.nr_themed_teal_2, ButtonBoxDrawable.CornerStyle.ALL)
                    userDismissable(true)
                    textColor(R.color.nr_button_text)
                    actionColor(R.color.nr_button_text)
                    setupDismissView(R.color.nr_button_text)
                }

                Type.ERROR_EXCLAIM_OUTSIDE -> {
                    background = ButtonBoxDrawable(getContext(), R.color.nr_bg, ButtonBoxDrawable.CornerStyle.ALL)
                    userDismissable(false)
                    textColor(R.color.nr_themed_grey_1)
                    actionColor(R.color.nr_themed_teal_2_clickable)
                    setupErrorView(R.color.nr_themed_apricot_1)
                }

                Type.DEFAULT_OUTSIDE -> {
                    background = ButtonBoxDrawable(getContext(), R.color.nr_bg, ButtonBoxDrawable.CornerStyle.ALL)
                    userDismissable(false)
                    textColor(R.color.nr_themed_grey_1)
                    actionColor(R.color.nr_themed_teal_2_clickable)
                }

                Type.DEFAULT -> {
                    background = ButtonBoxDrawable(getContext(), R.color.nr_themed_teal_2, ButtonBoxDrawable.CornerStyle.ALL)
                    userDismissable(false)
                    textColor(R.color.nr_button_text)
                    actionColor(R.color.nr_button_text)
                }

                else -> {
                    background = ButtonBoxDrawable(getContext(), R.color.nr_themed_teal_2, ButtonBoxDrawable.CornerStyle.ALL)
                    userDismissable(false)
                    textColor(R.color.nr_button_text)
                    actionColor(R.color.nr_button_text)
                }
            }

            root!!.setBackground(background)
            return this
        }

        private fun setupDismissView(@ColorRes iconColors: Int) {
            setIcon(R.drawable.ic_nr_close_x_mini, iconColors)
            icon!!.setOnClickListener(OnClickListener { v: View? -> dismissMessage(DismissReason.USER) })
            icon!!.setContentDescription(getContext().getResources().getText(R.string.ic_close))
        }

        private fun setupErrorView(@ColorRes iconColors: Int) {
            setIcon(R.drawable.ic_nr_error_mini, iconColors)
            icon!!.setContentDescription(null)
            icon!!.setOnClickListener(OnClickListener { v: View? -> reportError() })
            root!!.setOnLongClickListener(OnLongClickListener { v: View? ->
                reportError()
                false
            })
        }

        private fun reportError() {
            errorReporter.reportError(
                context,
                "Error: " + title!!.text.toString() + " " + message!!.text.toString(),
                null)
        }

        private fun setIcon(@DrawableRes iconRes: Int, @ColorRes iconColors: Int) {
            icon!!.setVisibility(VISIBLE)
            icon!!.setImageDrawable(VectorDrawableCompat.create(getResources(), iconRes, null))
            icon!!.setVisualMarginStart(R.dimen.nr_space_md)
            icon!!.setVisualMarginEnd(R.dimen.nr_space_md)
            icon!!.setDrawableColor(get(getContext(), iconColors))
        }

        private fun textColor(textColors: Int) {
            title!!.setTextColor(get(getContext(), textColors))
            message!!.setTextColor(get(getContext(), textColors))
        }

        private fun actionColor(actionColors: Int) {
            action!!.setTextColor(get(getContext(), actionColors))
        }

        fun onDismiss(listener: OnDismissListener?): Binder {
            onDismissListener = listener
            return this
        }

        fun onAction(@StringRes actionText: Int, listener: OnClickListener?): Binder {
            return onAction(actionText, null, listener)
        }

        fun onAction(
            @StringRes actionText: Int,
            uiIdentifier: String?,
            listener: OnClickListener?
        ): Binder {
            action!!.setTextAndUpdateEnUsLabel(actionText)
            action!!.setOnClickListener(listener)
            action!!.uiEntityIdentifier = uiIdentifier
            if (actionText == 0 || listener == null) {
                action!!.setVisibility(GONE)
            } else {
                action!!.setVisibility(VISIBLE)
            }
            return this
        }

        fun error(t: Throwable?): Binder {
            this.error = t
            return this
        }

        fun title(`val`: CharSequence?): Binder {
            title!!.setTextOrHide(`val`)
            return this
        }

        fun message(`val`: CharSequence?): Binder {
            message!!.setTextOrHide(`val`)
            return this
        }

        fun singleLineMessage(singleLine: Boolean): Binder {
            message!!.setSingleLine(singleLine)
            return this
        }

        fun show() {
            displayMessage()
        }

        fun dismiss() {
            dismissMessage(DismissReason.PROGRAMMATIC)
        }
    }

    companion object {
        private const val FADE_ANIM_MS = 500

        /**
         * A static reference to the currently shown AppSnackbar, to prevent multiples stacked on top of each other.
         * This is a WeakReference to avoid it sticking around after an Activity has been destroyed and leaking the Activity.
         */
        private var currentBar: java.lang.ref.WeakReference<AppSnackbar?>? = null

        fun init(reporter: ErrorReporter) {
            errorReporter = reporter
        }

        private var errorReporter =
            ErrorReporter { context: Context?, message: String?, t: Throwable? -> } // empty implementation

        fun make(
            activity: Activity,
            type: Type,
            anchor: View?,
            message: CharSequence?,
            listener: OnDismissListener?
        ): AppSnackbar {
            return make(activity, type, anchor, message, listener, 0, null, null)
        }

        fun make(
            activity: Activity,
            type: Type,
            message: CharSequence?,
            listener: OnDismissListener?
        ): AppSnackbar {
            return make(activity, type, null, message, listener, 0, null, null)
        }

        fun make(
            activity: Activity,
            type: Type,
            message: CharSequence?,
            listener: OnDismissListener?,
            @StringRes actionText: Int,
            actionListener: OnClickListener?
        ): AppSnackbar {
            return make(activity, type, message, listener, actionText, null, actionListener)
        }

        fun make(
            activity: Activity,
            type: Type,
            message: CharSequence?,
            listener: OnDismissListener?,
            @StringRes actionText: Int,
            actionIdentifier: String?,
            actionListener: OnClickListener?
        ): AppSnackbar {
            return make(
                activity,
                type,
                null,
                message,
                listener,
                actionText,
                actionIdentifier,
                actionListener
            )
        }

        /**
         * Creates a AppSnackbar and attaches it to the provided Activity's root content view.
         * @param activity          The current Activity context.
         * @param type              The type of notification (ERROR_DISMISSABLE, ERROR_EXCLAIM, DEFAULT)
         * @param anchor            An optional anchor view, which will add extra margin to the bottom of the notification.  Useful for placing it above bottom aligned navigation / buttons.
         * @param message           The message to display.
         * @param onDismissListener An optional listener to trigger on dismiss of the view.
         * @param actionText        Text for an optional "action button" which appears right aligned in the view. Both text and listener must be nonnull for it to appear.
         * @param actionIdentifier  UI entity identifier for the "action button".
         * @param actionListener    A click listener for the "action button".
         */
        fun make(
            activity: Activity,
            type: Type,
            anchor: View?,
            message: CharSequence?,
            onDismissListener: OnDismissListener?,
            @StringRes actionText: Int,
            actionIdentifier: String?,
            actionListener: OnClickListener?
        ): AppSnackbar {
            dismissCurrent()
            return makeInternal(
                activity,
                type,
                anchor,
                message,
                onDismissListener,
                actionText,
                actionIdentifier,
                actionListener
            )
        }

        val current: AppSnackbar?
            get() = if (currentBar == null) null else currentBar!!.get()

        fun dismissCurrent() {
            if (currentBar != null && currentBar!!.get() != null) {
                currentBar!!.get()!!.bind().dismiss()
                currentBar = null
            }
        }

        private fun getNotificationViewGroup(activity: Activity, anchor: View?): ViewGroup {
            if (anchor == null || anchor.getParent() !is ViewGroup) {
                return activity.findViewById<ViewGroup>(android.R.id.content)
            } else {
                return anchor.getParent() as ViewGroup
            }
        }

        /**
         * Creates a customized [ViewGroup.LayoutParams] depending on the parent View of the anchor in order to position the notification directly above the anchor (with margin).
         * TODO implement TooltipView instead. See [com.neverreader.sdk.util.view.tooltip] for example usage.
         */
        private fun getNotificationLayoutParams(
            activity: Activity,
            anchor: View?,
            notificationParent: ViewGroup
        ): MarginLayoutParams {
            val params: MarginLayoutParams
            val margin = activity.getResources().getDimension(R.dimen.nr_space_sm).toInt()

            if (notificationParent.getId() == NO_ID) {
                notificationParent.setId(generateViewId())
            }

            if (anchor != null && anchor.getId() == NO_ID) {
                anchor.setId(generateViewId())
            }

            if (notificationParent is FrameLayout) {
                params =
                    FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                params.gravity = Gravity.BOTTOM
                params.setMargins(
                    margin,
                    margin,
                    margin,
                    margin + (if (anchor == null) 0 else anchor.getHeight())
                ) // if anchor is null here that means android.R.id.content was used as the parent (which is a FrameLayout)
            } else if (notificationParent is ConstraintLayout) {
                params = ConstraintLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT)
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                params.bottomToTop = anchor!!.getId()
                params.setMargins(margin, margin, margin, margin)
            } else if (notificationParent is RelativeLayout) {
                params = RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                params.addRule(RelativeLayout.ABOVE, anchor!!.getId())
                params.setMargins(margin, margin, margin, margin)
            } else if (notificationParent is CoordinatorLayout) {
                params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                params.setMargins(
                    margin,
                    notificationParent.getHeight() - anchor!!.getHeight() - activity.getResources()
                        .getDimension(
                            R.dimen.nr_snackbar_height
                        ).toInt() - margin,
                    margin,
                    margin
                )
            } else {
                throw UnsupportedOperationException("The anchor's ViewGroup is not supported for AppSnackbar.")
            }
            return params
        }

        private fun makeInternal(
            activity: Activity,
            type: Type,
            anchor: View?,
            message: CharSequence?,
            listener: OnDismissListener?,
            @StringRes actionText: Int,
            actionIdentifier: String?,
            actionListener: OnClickListener?
        ): AppSnackbar {
            val bar = AppSnackbar(activity)

            setAnchor(activity, bar, anchor)

            bar.bind().onDismiss(AppSnackbar.OnDismissListener { reason: DismissReason? ->
                // hijack user supplied listener with our own to remove the view from decorview
                getNotificationViewGroup(activity, anchor).removeView(bar)
                if (listener != null) {
                    listener.onDismiss(reason)
                }
            }).type(type).onAction(actionText, actionIdentifier, OnClickListener { v ->
                bar.bind().dismiss() // dismiss on click
                if (actionListener != null) {
                    actionListener.onClick(v)
                }
            }).message(message)

            // GONE until show() is called
            bar.setVisibility(GONE)

            currentBar = java.lang.ref.WeakReference<AppSnackbar?>(bar)

            return bar
        }

        /**
         * Sets the anchor of the given AppSnackbar to the provided View.
         *
         * @param activity The Activity context.
         * @param bar The AppSnackbar to reposition.
         * @param anchor A View on which to anchor the AppSnackbar.
         */
        fun setAnchor(activity: Activity, bar: AppSnackbar, anchor: View?) {
            if (bar.getParent() != null) {
                (bar.getParent() as ViewGroup).removeView(bar)
            }

            val root: ViewGroup = getNotificationViewGroup(activity, anchor)

            val params: MarginLayoutParams = getNotificationLayoutParams(activity, anchor, root)

            // set provided margins as padding on the Snackbar instead, so swipe dismiss isn't cut off on the sides
            bar.setPadding(
                params.leftMargin,
                params.topMargin,
                params.rightMargin,
                params.bottomMargin
            )
            params.setMargins(0, 0, 0, 0)

            bar.setLayoutParams(params)
            root.addView(bar)
        }
    }
}
