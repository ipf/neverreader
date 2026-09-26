package com.neverreader.util.java

import android.content.Context
import java.util.Locale
import com.neverreader.app.App
import com.neverreader.app.R

object BytesUtil {
    private const val DEFAULT_PER_ITEM_AUTO_KB: Long = 444
    private const val DEFAULT_PER_ITEM_ARTICLE_ONLY_KB: Long = 333

    const val KB: Double = 1024.0
    val MB: Double = (1024L * 1024L).toDouble()
    val GB: Double = (1024L * 1024L * 1024L).toDouble()

    /**
     * Returns the bytes of a kb value. For example, 100.5 kb returns 102,912 bytes.
     * @param value
     * @return
     */
    fun kbToBytes(kb: Float): Long {
        return (kb * KB).toLong()
    }

    /**
     * Returns the bytes of a mb value. For example, 100.5 mb returns 105,381,888 bytes.
     * @param value
     * @return
     */
    fun mbToBytes(mb: Float): Long {
        return (mb * MB).toLong()
    }

    /**
     * Returns the bytes of a gb value. For example, 100.5 gb returns 107,911,053,312 bytes.
     * @param value
     * @return
     */
    fun gbToBytes(gb: Float): Long {
        return (gb * GB).toLong()
    }

    /**
     * Returns the kb of the bytes. For example, 102,912 bytes return 100.5 kb.
     * @param bytes
     * @return
     */
    fun bytesToKb(bytes: Long): Double {
        return bytes / KB
    }

    /**
     * Returns the mb of the bytes. For example, 105,381,888 bytes return 100.5 mb.
     * @param bytes
     * @return
     */
    fun bytesToMb(bytes: Long): Double {
        return bytes / MB
    }

    /**
     * Returns the gb of the bytes. For example, 107,911,053,312 bytes return 100.5 gb.
     * @param bytes
     * @return
     */
    fun bytesToGb(bytes: Long): Double {
        return bytes / GB
    }

    val averageBytesPerItem: Long
        // REVIEW use their actual averages to be more accurate per person ocne they have saved enough items?
        get() {
            return kbToBytes(DEFAULT_PER_ITEM_AUTO_KB.toFloat())
        }

    fun bytesToCleanString(context: Context, bytes: Long): String {
        if (bytesToMb(bytes) < 1000) {
            return bytesToMb(bytes).toInt()
                .toString() + " " + context.getString(R.string.setting_cache_mb)
        } else {
            return String.format(
                Locale.getDefault(),
                "%.1f",
                bytesToGb(bytes)
            ) + " " + context.getString(R.string.setting_cache_gb)
        }
    }
}
