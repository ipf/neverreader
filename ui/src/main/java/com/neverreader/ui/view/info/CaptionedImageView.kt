package com.neverreader.ui.view.info

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neverreader.ui.R
import com.neverreader.ui.view.visualmargin.VisualMarginConstraintLayout

/**
 * A commonly used mock up of image, title and short text used in a lot of onboarding and intro like ui.
 *
 *
 * This view is meant to have either a fixed width or a MATCH_PARENT width. It doesn't have an intrinsic width and won't work with WRAP_CONTENT.
 */
class CaptionedImageView : VisualMarginConstraintLayout {
    private val binder: Binder = Binder()

    private var image: ImageView? = null
    private var captionWrap: ViewGroup? = null
    private var title: TextView? = null
    private var text: TextView? = null

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
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
        LayoutInflater.from(getContext()).inflate(R.layout.view_captioned_image, this, true)
        captionWrap = findViewById<ViewGroup>(R.id.caption_wrap)
        image = findViewById<ImageView>(R.id.image)
        title = findViewById<TextView>(R.id.title)
        text = findViewById<TextView>(R.id.text)
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            image(null)
            title(null)
            text(null)
            captionHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
            return this
        }

        fun image(drawable: Drawable?): Binder {
            image!!.setImageDrawable(drawable)
            return this
        }

        fun image(@DrawableRes imageId: Int): Binder {
            image!!.setImageResource(imageId)
            return this
        }

        fun title(@StringRes resId: Int): Binder {
            title!!.setText(resId)
            return this
        }

        fun text(@StringRes resId: Int): Binder {
            text!!.setText(resId)
            return this
        }

        fun title(`val`: CharSequence?): Binder {
            title!!.setText(`val`)
            return this
        }

        fun text(`val`: CharSequence?): Binder {
            text!!.setText(`val`)
            return this
        }

        /**
         * This view is often used within a horizontal pager, which requires setting a consistent height
         * for all of the views based on the tallest item.  This method allows setting the absolute
         * height of the title and text views.  These views will align to the top if any excess space
         * exists.
         */
        fun captionHeight(height: Int): Binder {
            val params = captionWrap!!.getLayoutParams()
            params.height = height
            captionWrap!!.setLayoutParams(params)
            return this
        }
    }
}
