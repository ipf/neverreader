package com.neverreader.util.android.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.widget.ImageView

/*
* An ImageView that calls recycle on old bitmaps when setting a new one.
*
*/

class RecyclingImageView : ImageView {
    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    override fun setImageBitmap(bitmap: Bitmap?) {
        val drawable = getDrawable()
        if (drawable != null && drawable is BitmapDrawable) {
            val oldBitmap = (getDrawable() as BitmapDrawable).getBitmap()
            if (oldBitmap != null) {
                oldBitmap.recycle()
            }
        }

        super.setImageBitmap(bitmap)
    }
}

