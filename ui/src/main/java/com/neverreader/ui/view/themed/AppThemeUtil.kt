package com.neverreader.ui.view.themed

import android.content.Context
import android.view.View
import com.neverreader.util.android.ContextUtil

/**
 * Helper for providing custom themes like a light and dark theme to an app, screen and/or views.
 * These themes will be available at the drawable state level, so you can define state lists
 * that will automatically update as your theme does, without requiring recreating activities or views,
 * just like pressed states.
 *
 * <h2>Setup</h2>
 * <h4>Define available themes as attributes</h4>
 *
 * Add attributes to values/attrs.xml like:
 *
 * <pre> `<declare-styleable name="appTheme">		<attr name="state_light" format="boolean" />		<attr name="state_dark" format="boolean" />	</declare-styleable> `</pre>
 *
 * <h4>Create Themed Views</h4>
 *
 * For any views that you will use custom theming, create subclasses that override this method:
 * (Note: there are already ThemedViews created for most View types)
 *
 * <pre> `protected int[] onCreateDrawableState(int extraSpace) {		final int[] state = super.onCreateDrawableState(extraSpace + 1);		mergeDrawableStates(state, AppThemeUtil.getState(this));		return state;	} `</pre>
 *
 * <h4>Set/Apply Themes</h4>
 *
 * To define an app wide theme that applies to all activities and views in the entire app,
 * have your [android.app.Application] implement [Themed].
 *
 *
 * To define the theme at the activity level, have your [android.app.Activity] implement [Themed].
 *
 *
 * To have the theme applied to only specific views, have your view or one of its parent views implement [Themed].
 *
 * <h2>Usage</h2>
 *
 * After that initial setup, then you can make custom state lists using your themes, for example:
 *
 * <pre> `<selector xmlns:android="http://schemas.android.com/apk/res/android"	xmlns:app="http://schemas.android.com/apk/res-auto">		<item		  app:state_dark="true"		  android:color="@color/dark_gray" />		<item		  android:color="@color/light_gray"/> </selector> `</pre>
 *
 * In this example, when the app is in dark mode, it will use dark_gray, and when in light mode, it will use light_gray.
 *
 *
 * You can then use these state list color or drawables normally in xml or at runtime.
 *
 *
 */
object AppThemeUtil {
    val EMPTY: IntArray = IntArray(0)

    fun getState(view: View): IntArray? {
        val themed = findThemed(view)
        if (themed != null) {
            return themed.getThemeState(view)
        } else {
            return EMPTY
        }
    }

    private fun findThemed(view: View): Themed? {
        // Check the view itself
        if (view is Themed) {
            return view as Themed
        }
        // Check its parent views
        var parent = view.parent
        while (parent is View) {
            if (parent is Themed) {
                return parent as Themed
            }
            parent = parent.parent
        }
        // Check its context (such as activities and application)
        return findThemed(view.context)
    }

    fun findThemed(context: Context): Themed? {
        val themed = ContextUtil.findContext(context, Themed::class.java)
        if (themed != null) {
            return themed
        }
        return ContextUtil.findContext(context.applicationContext, Themed::class.java)
    }
}
