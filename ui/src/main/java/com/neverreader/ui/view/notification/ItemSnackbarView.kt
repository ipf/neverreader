package com.neverreader.ui.view.notification

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.util.LazyBitmap
import com.neverreader.ui.util.LazyBitmapDrawable
import com.neverreader.ui.util.NestedColorStateList.get
import com.neverreader.ui.view.button.ButtonBoxDrawable
import com.neverreader.ui.view.item.ItemMetaView
import com.neverreader.ui.view.item.ItemThumbnailView

open class ItemSnackbarView : CoordinatorLayout {
    private var card: CardView? = null
    private var thumbnail: ItemThumbnailView? = null
    private var miniIcon: ImageView? = null
    private var featureTitle: TextView? = null
    private var meta: ItemMetaView? = null

    private var dismissBehavior: AppSwipeDismissBehavior<*>? = null

    private val binder: Binder = Binder()

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
        LayoutInflater.from(getContext()).inflate(R.layout.view_item_snackbar, this, true)
        val cardRadius = dpToPxInt(getContext(), 4f)

        findViewById<View>(R.id.root_view).setBackground(
            ButtonBoxDrawable(
                getContext(),
                R.color.nr_opaque_touchable_area,
                R.color.nr_item_snackbar_stroke,
                cardRadius.toFloat()
            )
        )

        card = findViewById<CardView>(R.id.card)
        thumbnail = findViewById<ItemThumbnailView>(R.id.item_thumbnail)
        miniIcon = findViewById<ImageView>(R.id.icon_mini)
        featureTitle = findViewById<TextView>(R.id.feature_title)
        meta = findViewById<ItemMetaView>(R.id.item_meta)

        card!!.setUseCompatPadding(true)
        card!!.setRadius(cardRadius.toFloat())
        card!!.setCardElevation(dpToPxInt(getContext(), 4f).toFloat())
        card!!.setCardBackgroundColor(get(getContext(), R.color.nr_bg))

        dismissBehavior = AppSwipeDismissBehavior<View>()
        dismissBehavior!!.setSwipeDirection(AppSwipeDismissBehavior.SWIPE_DIRECTION_ANY)

        val layoutParams = card!!.getLayoutParams() as LayoutParams
        layoutParams.setBehavior(dismissBehavior)

        setClipToPadding(false)
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            thumbnail(null)
            icon(0)
            featureTitle(null)
            onClick(null)
            onDismiss(null)
            meta()!!.clear()
                .titleMaxLines(1)
            return this
        }

        fun thumbnail(value: LazyBitmap?): Binder {
            thumbnail!!.setImageDrawable(if (value != null) LazyBitmapDrawable(value) else null)
            return this
        }

        fun icon(@DrawableRes iconRes: Int): Binder {
            miniIcon!!.setImageResource(iconRes)
            miniIcon!!.setVisibility(if (iconRes == 0) GONE else VISIBLE)
            return this
        }

        fun featureTitle(title: CharSequence?): Binder {
            featureTitle!!.setText(title)
            return this
        }

        fun onClick(listener: OnClickListener?): Binder {
            card!!.setOnClickListener(listener)
            return this
        }

        fun onDismiss(listener: AppSwipeDismissBehavior.OnDismissListener?): Binder {
            dismissBehavior!!.setListener(listener)
            return this
        }

        fun meta(): ItemMetaView.Binder? {
            return meta!!.bind()
        }
    }
}
