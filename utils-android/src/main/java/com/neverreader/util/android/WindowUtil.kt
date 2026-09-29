package com.neverreader.util.android

import android.util.Property
import android.view.Window

abstract class WindowUtil {
    class StatusBarColorProperty : Property<Window, Int>(Int::class.java, "statusBarColor") {
        override fun set(`object`: Window, value: Int) {
            `object`.statusBarColor = value
        }

        override fun get(`object`: Window): Int {
            return `object`.statusBarColor
        }
    }

    class NavigationBarColorProperty :
        Property<Window, Int>(Int::class.java, "navigationBarColor") {
        override fun set(`object`: Window, value: Int) {
            `object`.statusBarColor = value
        }

        override fun get(`object`: Window): Int {
            return `object`.navigationBarColor
        }
    }
}
