package com.neverreader.ui.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil
import com.neverreader.ui.util.NestedColorStateList
import com.neverreader.ui.view.button.BoxButton
import com.neverreader.ui.view.button.IconButton
import com.neverreader.ui.view.themed.ThemedConstraintLayout

/**
 * A view for displaying a standard themed "AppBar" (Toolbar, Actionbar, etc).
 *
 * Example usage:
 * ```xml
 * <com.neverreader.ui.view.AppBar
 *     android:id="@+id/appbar"
 *     app:leftIcon="up"
 *     android:title="This is a title"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 * ```
 *
 * Options for "app:leftAction" include none, up, and close.
 */
open class AppBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ThemedConstraintLayout(context, attrs, defStyleAttr) {

    private val minIconSize: Int = DimenUtil.dpToPxInt(context, 24f)
    private val rightPaddingDefault: Int = resources.getDimension(R.dimen.nr_side_grid).toInt()

    private val binder = Binder()

    private lateinit var leftIcon: IconButton
    private lateinit var title: TextView
    private lateinit var actions: ViewGroup
    private var divider: View? = null

    @LayoutRes
    protected fun layout(): Int = R.layout.view_app_bar

    @DrawableRes
    protected fun upRes(): Int = R.drawable.ic_nr_back_arrow_line

    @DrawableRes
    protected fun closeRes(): Int = R.drawable.ic_nr_close_x_line

    init {
        LayoutInflater.from(context).inflate(layout(), this, true)

        leftIcon = findViewById(R.id.leftIcon)
        title = findViewById(R.id.title)
        actions = findViewById(R.id.actions)
        divider = findViewById(R.id.divider)

        bind().clear()

        if (attrs != null) {
            val ta: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.AppBar)
            bind().title(ta.getText(R.styleable.AppBar_android_title))
            when (ta.getInt(R.styleable.AppBar_leftIcon, 1)) { // defaults to up arrow
                0 -> bind().withNoLeftIcon()
                1 -> bind().withUpArrow()
                2 -> bind().withCloseIcon()
            }
            bind().divider(ta.getBoolean(R.styleable.AppBar_bottomDivider, true))
            ta.recycle()
        }

        setBackgroundResource(R.drawable.cl_nr_bg)
    }

    fun bind(): Binder = binder

    val leftIconView: IconButton
        get() = leftIcon

    /**
     * Returns the action View at a specific index in the actions layout.
     */
    fun getActionView(index: Int): View = actions.getChildAt(index)

    inner class Binder {

        fun clear(): Binder {
            title(null)
            withUpArrow()
            divider(true)
            actions.removeAllViews()
            return this
        }

        /**
         * Whether to show or hide the bottom divider line.
         */
        fun divider(show: Boolean): Binder {
            // some AppBar extensions may not include a divider
            val divider = divider ?: return this
            divider.visibility = if (show) View.VISIBLE else View.GONE
            return this
        }

        /**
         * Sets a custom left icon for the AppBar.
         */
        fun leftIcon(@DrawableRes drawable: Int, @StringRes contentDescription: Int) {
            leftIcon(drawable, contentDescription, null)
        }

        fun leftIcon(@DrawableRes drawable: Int, @StringRes contentDescription: Int, uiEntityIdentifier: String?) {
            leftIcon.visibility = View.VISIBLE
            leftIcon.setImageResource(drawable)
            leftIcon.contentDescription = resources.getString(contentDescription)
            leftIcon.uiEntityIdentifier = uiEntityIdentifier
        }

        /**
         * Hides the left icon, aligning the title with the left of the AppBar.
         */
        fun withNoLeftIcon() {
            leftIcon.visibility = View.GONE
        }

        /**
         * Sets the AppBar to use an up left icon.
         */
        fun withUpArrow() {
            leftIcon(upRes(), R.string.ic_up)
        }

        /**
         * Sets the AppBar to use a close ("X") left icon.
         */
        fun withCloseIcon() {
            leftIcon(closeRes(), R.string.ic_close)
        }

        fun withCloseIcon(uiEntityIdentifier: String): Binder {
            leftIcon(closeRes(), R.string.ic_close, uiEntityIdentifier)
            return this
        }

        /**
         * Sets the [android.view.View.OnClickListener] for the left icon.
         */
        fun onLeftIconClick(listener: OnClickListener?): Binder {
            leftIcon.setOnClickListener(listener)
            return this
        }

        /**
         * Sets the title of the AppBar.
         */
        fun title(value: CharSequence?): Binder {
            title.text = value
            return this
        }

        /**
         * Sets the title of the AppBar.
         */
        fun title(@StringRes value: Int): Binder {
            title.text = resources.getText(value)
            return this
        }

        /**
         * Adds a new [IconButton] to the actions ViewGroup.
         */
        fun addIconAction(
            @DrawableRes drawable: Int,
            @StringRes contentDescription: Int,
            listener: OnClickListener?
        ): Binder {
            if (!viewExists(drawable)) {
                val action = IconButton(ContextThemeWrapper(context, R.style.App_IconButton))
                action.setImageResource(drawable)
                bindAction(action, resources.getString(contentDescription), drawable, listener)
                fixSmallIcon(action, action.drawable)
                action.setDrawableColor(NestedColorStateList.get(context, R.color.nr_themed_grey_1_clickable))
            }
            return this
        }

        /**
         * Adds a new [BoxButton] to the actions ViewGroup.
         */
        fun addButtonAction(@StringRes label: Int, listener: OnClickListener): Binder {
            if (!viewExists(label)) {
                val action = BoxButton(context)
                action.setText(label)
                bindAction(action, resources.getString(label), label, listener)
                addButtonVerticalMargin(action)
            }
            return this
        }

        /**
         * Checks if a view with an identical id has already been added to the actions. This prevents re-adding an action in certain situations,
         * such as configuration changes.
         */
        private fun viewExists(viewId: Int): Boolean {
            for (i in 0 until actions.childCount) {
                if (actions.getChildAt(i).id == viewId) {
                    return true
                }
            }
            return false
        }

        private fun addButtonVerticalMargin(view: View) {
            val params = view.layoutParams as LinearLayout.LayoutParams
            val verticalMargin = resources.getDimension(R.dimen.nr_space_sm).toInt()
            params.setMargins(params.leftMargin, verticalMargin, params.rightMargin, verticalMargin)
        }

        private fun bindAction(view: View, contentDescription: CharSequence, viewId: Int, listener: OnClickListener?) {
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            params.setMargins(resources.getDimension(R.dimen.nr_space_md).toInt(), 0, 0, 0)
            view.layoutParams = params
            view.contentDescription = contentDescription
            view.setOnClickListener(listener)
            view.id = viewId
            actions.addView(view)
            // reset the default right padding, in case a previously added icon action was smaller than 24dp
            actions.setPadding(0, 0, rightPaddingDefault, 0)
        }

        /**
         * Appbar action icons are generally 24dp in width (per Material design), the main exception being the overflow button.
         * The appbar uses WRAP_CONTENT for the action buttons, but this makes the overflow impossible to click because it is too thin.
         *
         * For this, we add left/right padding to any icon that is smaller than 24dp to make up the difference.
         * However, if the icon is the last icon in the group, it should still align with the right side of the appbar.
         */
        private fun fixSmallIcon(view: View, drawable: Drawable) {
            val iconWidth = drawable.intrinsicWidth
            if (iconWidth < minIconSize) {
                val padding = (minIconSize - iconWidth) / 2
                view.setPadding(padding, 0, padding, 0)
                actions.setPadding(0, 0, rightPaddingDefault - padding, 0)
            }
        }
    }
}
