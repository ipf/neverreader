package com.neverreader.ui.util

import android.R
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.util.SparseArray
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.neverreader.util.java.Logs
import com.neverreader.util.java.RangeF
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

/**
 * Allows color state lists to reference other color state lists in xml.
 *
 *
 * Write your color state xml like normal. Reference other state lists just like normal color references like `android:color="@color/your_state_state_list".
 * Then load them at runtime with [.get], which will return a normal [ColorStateList].
 *
 *
 * Note, that state rules are included on any referenced rules.
 * So for example, say you a color state list called "amber_selector" that looks like this:
 *
 * <pre>&lt;?xml version=&quot;1.0&quot; encoding=&quot;utf-8&quot;?&gt; &lt;selector xmlns:android=&quot;http://schemas.android.com/apk/res/android&quot;     xmlns:app=&quot;http://schemas.android.com/apk/res-auto&quot;&gt;     &lt;item app:state_dark=&quot;true&quot; android:drawable=&quot;@color/nr_dm_amber&quot; /&gt;     &lt;item android:drawable=&quot;@color/nr_amber&quot; /&gt; &lt;/selector&gt; </pre>
 *
 * If you reference that with a rule like:
 *
 * `<item android:state_checked="true" android:drawable="@color/amber_selector" />`
 *
 * It will be the equivalent of having written:
 *
 * <pre>&lt;item android:state_checked=&quot;true&quot; app:state_dark=&quot;true&quot; android:drawable=&quot;@color/nr_dm_amber&quot; /&gt;     &lt;item android:state_checked=&quot;true&quot; android:drawable=&quot;@color/nr_amber&quot; /&gt; </pre>
 *
 */
object NestedColorStateList {
    private val builder = Builder()
    private val cache = SparseArray<ColorStateList?>()

    /**
     * This must be invoked from the ui thread.
     */
    @JvmStatic
    fun get(context: Context, resId: Int): ColorStateList? {
        // TODO force ui thread or synchronize
        // TODO we don't have any config dependant colors yet, but if we start doing that, we need to remove things from the cache in certain cases.  See TypedValue.changingConfigurations
        // TODO improve performance, though with the cache, this isn't urgent at all. the biggest gain we can do is from getting rid of usage of Collection and object classes and use primitive arrays, see ColorStateList.inflate and how they handle arrays for inspiration.
        //			another potential gain is using the cache for internal nested color state list look ups as well

        val cached = cache.get(resId)
        if (cached != null) {
            return cached
        }

        try {
            builder.recycle()

            val resources = context.getResources()
            resources.getValue(resId, builder.v, true)
            if (builder.v.type != TypedValue.TYPE_STRING) {
                // Single color
                return ContextCompat.getColorStateList(context, resId)
            }

            collectColors(builder, resources, resId, null)

            val colors = IntArray(builder.colors.size)
            val states = arrayOfNulls<IntArray>(colors.size)
            var i = 0
            val len = colors.size
            while (i < len) {
                colors[i] = builder.colors.get(i)!!
                states[i] = builder.states.get(i)
                i++
            }

            val result = ColorStateList(states, colors)

            cache.put(resId, result)
            return result
        } catch (t: Throwable) {
            Logs.printStackTrace(t)
            return null


            // Comment out above two lines and uncomment below to make layout preview work in Android Studio:
            // return new ColorStateList(new int[0][], new int[0]);
        }
    }

    /**
     * Parse a color state list xml, adding its colors/rules into the builder.
     *
     * @param into Where to add the rules
     * @param resources
     * @param resId The resource id of the color state list
     * @param parentStates Any rules to merge/add to all rules found in this color state list (optional, can be null to just add the rules as they are found)
     */
    @Throws(IOException::class, XmlPullParserException::class)
    private fun collectColors(
        into: Builder,
        resources: Resources,
        resId: Int,
        parentStates: MutableSet<Int>?
    ) {
        /*
			Inspirations for how to make this work are from ColorStateList.inflate, Resources.getColorStateList and similar framework methods
		 */
        val parser = resources.getXml(resId)
        var token: Int
        while ((parser.next().also { token = it }) != XmlPullParser.END_DOCUMENT) {
            if (token == XmlPullParser.START_TAG && "item" == parser.getName()) {
                val states: MutableSet<Int> = HashSet<Int>(parser.getAttributeCount() - 1)
                var nestedStateList = 0
                var rawColor: Int? = null
                var alpha = 1f

                var i = 0
                val count = parser.getAttributeCount()
                while (i < count) {
                    var attribute = parser.getAttributeNameResource(i)

                    if (attribute == R.attr.color) {
                        val color = parser.getAttributeIntValue(i, -1)
                        if (color != -1) {
                            // Directly declared color
                            rawColor = color
                        } else {
                            resources.getValue(
                                parser.getAttributeResourceValue(i, -1),
                                into.v,
                                true
                            )
                            if (into.v.type == TypedValue.TYPE_STRING) {
                                // ColorStateList
                                nestedStateList = into.v.resourceId
                            } else {
                                // Color resource
                                rawColor = into.v.data
                            }
                        }
                    } else if (attribute == R.attr.alpha) {
                        alpha = parser.getAttributeFloatValue(i, 1f)
                    } else {
                        if (!parser.getAttributeBooleanValue(i, false)) {
                            attribute = -attribute
                        }
                        states.add(attribute)
                    }
                    i++
                }

                if (parentStates != null) {
                    states.addAll(parentStates)
                }

                if (nestedStateList != 0) {
                    collectColors(into, resources, nestedStateList, states)
                } else {
                    into.colors.add(NestedColorStateList.modulateColorAlpha(rawColor!!, alpha))

                    val array = IntArray(states.size)
                    var i = 0
                    for (state in states) {
                        array[i++] = state
                    }
                    into.states.add(array)
                }
            }
        }
    }

    /**
     * Based on source of ColorStateList's implementation
     */
    private fun modulateColorAlpha(baseColor: Int, alphaMod: Float): Int {
        if (alphaMod == 1.0f) {
            return baseColor
        }

        val baseAlpha = Color.alpha(baseColor)
        val alpha = RangeF.constrain(0f, 255f, baseAlpha * alphaMod + 0.5f).toInt()
        return (baseColor and 0xFFFFFF) or (alpha shl 24)
    }

    private class Builder {
        internal val states = ArrayList<IntArray?>()
        internal val colors = ArrayList<Int?>()
        internal val v = TypedValue()

        fun recycle() {
            states.clear()
            colors.clear()
        }
    }
}
