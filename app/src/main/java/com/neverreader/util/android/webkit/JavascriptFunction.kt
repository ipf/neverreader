package com.neverreader.util.android.webkit

import android.webkit.ValueCallback
import android.webkit.WebView
import com.fasterxml.jackson.databind.JsonNode
import com.neverreader.app.App
import com.neverreader.app.App.Companion.getContext as appContextFn
import com.neverreader.util.java.StringBuilders
import com.neverreader.util.java.StringBuilders.get
import org.apache.commons.lang3.StringEscapeUtils

/**
 * A function that can be invoked in Javascript.
 *
 *
 * <h3>To use:</h3>
 * Choose one of the constructors and then add optional parameter values with the various `value()` methods. They will be included in the parameters of the function in the order in which you call `value()`.
 *
 *
 * To invoke it, call [.execute]. `execute()` may be used multiple times but after executing the function, it may not longer be changed. This means if you attempt to call a `value()` method after `execute()`
 * an exception will be thrown.
 * @author max
 */
class JavascriptFunction(`object`: String?, functionName: String?) {
    private var mBuilder: StringBuilder?
    private var mQuery: String? = null
    private var mHasParams = false

    /**
     * @param functionName The method name. For example, "alert" would correspond to `alert()`
     */
    constructor(functionName: String?) : this(null, functionName)

    /**
     * @param object The object to invoke the function on. For example, "video" from this example: `video.play()`
     * @param functionName The method name. For example, "play" from this example: `video.play()`
     */
    init {
        mBuilder = get()

        if (`object` != null) {
            mBuilder!!.append(`object`)
                .append(".")
        }
        mBuilder!!.append(functionName)
            .append("(")
    }

    /**
     * Adds a value to the function's parameters. For example, adding "Hello World" would add it to the function like `object.functionName('Hello World')`
     *
     *
     * Parameters are added in the order in which these `value()` methods are called.
     * @param value
     * @param escape true if this string should [.escapeForSingleQuote], false if already escaped. If you are passing a huge string here, you should escape it ahead of time asynchoronusly so it doesn't block the ui thread.
     * @return This [JavascriptFunction] for chaining calls.
     */
    /**
     * @see {@link .value
     */
    @JvmOverloads
    fun value(value: String, escape: Boolean = true): JavascriptFunction {
        var value = value
        prepareNextParam()

        if (escape) {
            value = escapeForSingleQuote(value)
        }

        mBuilder!!.append("'")
            .append(value)
            .append("'")

        return this
    }

    fun valueJsonString(value: String): JavascriptFunction {
        prepareNextParam()
        mBuilder!!.append(escapeJsonForString(value))
        return this
    }

    /**
     * Adds a value to the function's parameters. For example, adding {"key":"value"} would add it to the function like `object.functionName({"key":"value"})`
     *
     *
     * Parameters are added in the order in which these `value()` methods are called.
     * @param value
     * @return This [JavascriptFunction] for chaining calls.
     */
    fun value(value: JsonNode): JavascriptFunction {
        prepareNextParam()

        val string: String = escapeJsonForString(value)

        mBuilder!!.append(string)
        return this
    }

    /**
     * Adds a value to the function's parameters. For example, adding `108` would add it to the function like `object.functionName(108)`
     *
     *
     * Parameters are added in the order in which these `value()` methods are called.
     * @param value
     * @return This [JavascriptFunction] for chaining calls.
     */
    fun value(value: Int): JavascriptFunction {
        prepareNextParam()
        mBuilder!!.append(value)
        return this
    }

    /**
     * Adds a value to the function's parameters. For example, adding `4815162342` would add it to the function like `object.functionName(4815162342)`
     *
     *
     * Parameters are added in the order in which these `value()` methods are called.
     * @param value
     * @return This [JavascriptFunction] for chaining calls.
     */
    fun value(value: Long): JavascriptFunction {
        prepareNextParam()
        mBuilder!!.append(value)
        return this
    }

    /**
     * Adds a value to the function's parameters. For example, adding `108.21` would add it to the function like `object.functionName(108.21)`
     *
     *
     * Parameters are added in the order in which these `value()` methods are called.
     * @param value
     * @return This [JavascriptFunction] for chaining calls.
     */
    fun value(value: Float): JavascriptFunction {
        prepareNextParam()
        mBuilder!!.append(value)
        return this
    }

    /**
     * Adds a value to the function's parameters. For example, adding `48.15162342` would add it to the function like `object.functionName(48.15162342)`
     *
     *
     * Parameters are added in the order in which these `value()` methods are called.
     * @param value
     * @return This [JavascriptFunction] for chaining calls.
     */
    fun value(value: Double): JavascriptFunction {
        prepareNextParam()
        mBuilder!!.append(value)
        return this
    }

    /**
     * Adds a value to the function's parameters. For example, adding `true` would add it to the function like `object.functionName(true)`
     *
     *
     * Parameters are added in the order in which these `value()` methods are called.
     * @param value
     * @return This [JavascriptFunction] for chaining calls.
     */
    fun value(value: Boolean): JavascriptFunction {
        prepareNextParam()
        mBuilder!!.append(if (value) "true" else "false")
        return this
    }

    /**
     * Invokes the function constructed thus far on the provided WebView. If not on the UI Thread, it will be posted to it.
     *
     *
     * **Note:** While you may call execute multiple times, after calling this method, no further changes may be made to this function.
     * @param webview
     */
    fun execute(webview: WebView) {
        App.from(appContextFn()!!)!!.threads().runOrPostOnUiThread(Runnable {
            if (mQuery == null) {
                mBuilder!!.append(");")
                mQuery = mBuilder.toString()
                StringBuilders.recycle(mBuilder!!)
                mBuilder = null
            }
            webview.evaluateJavascript(mQuery!!, ValueCallback { s: String? -> })
            if (webview is JavascriptMethodListener) {
                (webview as JavascriptMethodListener).onJavascriptExecuted()
            }
        })
    }

    private fun prepareNextParam() {
        if (mHasParams) {
            mBuilder!!.append(", ")
        } else {
            mHasParams = true
        }
    }

    interface JavascriptMethodListener {
        /** Invoked after a [JavascriptFunction] has been executed within this WebView. */
        fun onJavascriptExecuted()
    }

    companion object {
        fun escapeForSingleQuote(value: String): String {
            // Properly escape special characters or transform encoded ' into ' so we can escape them.
            var value = value
            value = StringEscapeUtils.escapeJava(value)
            value = value.replace("(?<!\\\\)'".toRegex(), "\\\\'") // Escape ' if not escaped
            return value
        }

        fun escapeJsonForString(value: JsonNode): String {
            return escapeJsonForString(value.toString())
        }

        fun escapeJsonForString(string: String): String {
            // Escape some characters that break things if they aren't already escaped.
            var string = string
            val notEscapedPrefix = "(?<!\\\\)"
            string = string.replace((notEscapedPrefix + "\u2028").toRegex(), "\\\\u2028")
            string = string.replace((notEscapedPrefix + "\u2029").toRegex(), "\\\\u2029")
            return string
        }
    }
}
