package com.neverreader.ui.util

import android.graphics.Bitmap

/**
 * An asynchronously loading [Bitmap].
 * Mostly for use in [LazyBitmapDrawable].
 */
interface LazyBitmap {
    fun fill(widthPx: Int, heightPx: Int, loaded: Loaded?, canceller: Canceller?)

    //	void fillRatio(int portWidth, int portHeight, int squareWidth, int squareHeight, int landWidth, int landHeight, Loaded loaded, Canceller canceller);
    fun interface Loaded {
        fun onBitmapLoaded(bitmap: Bitmap?)
    }

    class Canceller {
        var isCancelled: Boolean = false
            private set

        fun cancel() {
            this.isCancelled = true
        }

        companion object {
            fun cancelAndRenew(canceller: Canceller?): Canceller {
                canceller?.cancel()
                return Canceller()
            }
        }
    }
}
