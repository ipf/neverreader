package com.neverreader.ui.view.item

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.neverreader.ui.R
import com.neverreader.ui.analytics.UiEntityable
import com.neverreader.ui.util.CheckableHelper
import com.neverreader.ui.view.checkable.CheckableConstraintLayout
import com.neverreader.ui.view.themed.ThemedTextView

/**
 *
 */
class SaveButton : CheckableConstraintLayout {
    private val binder = Binder(this)
    private var checkedListener: CheckableHelper.OnCheckedChangeListener? = null

    private var label: ThemedTextView? = null

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
        LayoutInflater.from(getContext()).inflate(R.layout.view_save, this, true)
        findViewById<View>(R.id.save_icon).setLongClickable(false) // By default, IconButton's have tooltips on long press, we don't want this icon to take touches.
        label = findViewById<ThemedTextView>(R.id.save_label)
        setCheckable(true)
        setBackgroundResource(R.drawable.nr_ripple_borderless)
        checkedListener =
            CheckableHelper.OnCheckedChangeListener { view: View?, isChecked: Boolean ->
                if (isChecked != binder.listener.onSaveButtonClicked(this@SaveButton, isChecked)) {
                    bind().setSaved(!isChecked)
                    return@OnCheckedChangeListener  // Don't change.
                }
                updateSaveLabel()
            }
        setOnCheckedChangeListener(checkedListener)
        bind().clear()
        engageable.uiEntityType = UiEntityable.Type.BUTTON
    }

    private fun updateSaveLabel() {
        label!!.setTextAndUpdateEnUsLabel(if (isChecked()) R.string.ic_saved else R.string.ic_save)
        setContentDescription(label!!.getText())
    }

    override val uiEntityLabel: String?
        get() = label!!.uiEntityLabel

    fun bind(): Binder {
        return binder
    }

    class Binder(private val view: SaveButton) {
        var listener: OnSaveButtonClickListener = NO_OP_LISTENER

        fun clear(): Binder {
            label(true)
            setSaved(false)
            setOnSaveButtonClickListener(null)
            return this
        }

        fun label(visible: Boolean): Binder {
            view.label!!.setVisibility(if (visible) VISIBLE else GONE)
            return this
        }

        fun setSaved(isSaved: Boolean): Binder {
            view.setOnCheckedChangeListener(null) // Clear the listener while changing bindings, so it only triggers from actual clicks.
            view.setChecked(isSaved)
            view.setOnCheckedChangeListener(view.checkedListener)
            view.updateSaveLabel()
            return this
        }

        fun setOnSaveButtonClickListener(listener: OnSaveButtonClickListener?): Binder {
            this.listener = if (listener != null) listener else NO_OP_LISTENER
            return this
        }

        fun interface OnSaveButtonClickListener {
            /**
             * @param saved true if the button was clicked and wants to move to the saved state. false if wants to become not saved.
             * @return the state the button should be in
             */
            fun onSaveButtonClicked(view: SaveButton?, saved: Boolean): Boolean
        }

        companion object {
            private val NO_OP_LISTENER =
                OnSaveButtonClickListener { view: SaveButton?, saved: Boolean -> saved }
        }
    }
}
