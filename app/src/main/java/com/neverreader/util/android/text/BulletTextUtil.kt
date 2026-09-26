package com.neverreader.util.android.text

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan

object BulletTextUtil {
    /**
     * Returns a CharSequence containing a bulleted and properly indented list.
     *
     * @param leadingMargin In pixels, the space between the left edge of the bullet and the left edge of the text.
     * @param lines An array of CharSequences. Each CharSequences will be a separate line/bullet-point.
     * @return
     */
    fun makeBulletList(leadingMargin: Int, vararg lines: CharSequence?): CharSequence {
        val sb = SpannableStringBuilder()
        for (i in lines.indices) {
            val line: CharSequence = lines[i].toString() + (if (i < lines.size - 1) "\n" else "")
            val spannable: Spannable = SpannableStringBuilder(line)
            spannable.setSpan(
                BulletSpan(leadingMargin),
                0,
                spannable.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            sb.append(spannable)
        }
        return sb
    }
}
