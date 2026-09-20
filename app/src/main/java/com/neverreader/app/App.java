package com.neverreader.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.res.Configuration;

import com.ideashower.readitlater.R;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.neverreader.app.reader.internal.article.DisplaySettingsManager;
import com.neverreader.app.settings.SystemDarkTheme;
import com.neverreader.app.settings.Theme;
import com.neverreader.app.settings.UserAgent;
import com.neverreader.app.settings.rotation.RotationLock;
import com.neverreader.backend.sync.SyncWorker;
import com.neverreader.repository.BookmarkRepository;
import com.neverreader.sdk.http.HttpClientDelegate;
import com.neverreader.sdk.image.ImageCache;
import com.neverreader.sdk.preferences.AppPrefs;
import com.neverreader.sdk.util.AbsPocketActivity;
import com.neverreader.sdk.util.wakelock.WakeLockManager;
import com.neverreader.ui.view.notification.PktSnackbar;
import com.neverreader.util.android.Clipboard;
import com.neverreader.util.android.IntentUtils;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@SuppressWarnings("unused")
@HiltAndroidApp
public class App extends Application implements AppGraph {

	@Inject AppThreads appThreads;
	@Inject ImageCache imageCache;
	@Inject WakeLockManager wakeLockManager;
	@Inject RotationLock rotationLock;
	@Inject DisplaySettingsManager displaySettingsManager;
	@Inject UserAgent userAgent;
	@Inject ActivityMonitor activityMonitor;
	@Inject SystemDarkTheme systemDarkTheme;
	@Inject Theme theme;
	@Inject Clipboard clipboard;
	@Inject AppLifecycleEventDispatcher appLifecycleEventDispatcher;
	@Inject HttpClientDelegate httpClientDelegate;
	@Inject AppPrefs appPrefs;
	@Inject Device device;
	@Inject AppOpen appOpen;
	@Inject BookmarkRepository bookmarkRepository;

	// App State
    private static App sContext;

	/**
	 * The currently open Activity, if any.
	 */
	private static AbsPocketActivity sActivityContext;

	private static boolean sIsUserPresent = false;

	private static Set<OnUserPresenceChangedListener> sOnUserPresenceChangedListeners = new HashSet<>();

	/** @deprecated Avoid using if possible, instead have your class receive a context (or whatever it is using a context to retrieve) as a dependency */
	@Deprecated
	public static Context getContext(){
		return sContext;
	}

	@Override
	public void onCreate() {
		sContext = this;
		AndroidThreeTen.init(this);
		super.onCreate();

		com.neverreader.ui.view.notification.PktSnackbar.ErrorReporter noOpReporter =
				(context, message, throwable) -> { };
		PktSnackbar.init(noOpReporter);
		SyncWorker.schedule(this);
	}

	@Override public AppMode mode() { return BuildConfig.DEBUG ? AppMode.DEV : AppMode.PRODUCTION; }
	@Override public Theme theme() { return theme; }
	@Override public SystemDarkTheme systemDarkTheme() { return systemDarkTheme; }
	@Override public ActivityMonitor activities() { return activityMonitor; }
	@Override public AppLifecycleEventDispatcher dispatcher() { return appLifecycleEventDispatcher; }
	@Override public AppThreads threads() { return appThreads; }
	@Override public Clipboard clipboard() { return clipboard; }
	@Override public ImageCache imageCache() { return imageCache; }
	@Override public HttpClientDelegate http() { return httpClientDelegate; }
	@Override public AppPrefs prefs() { return appPrefs; }
	@Override public DisplaySettingsManager displaySettings() { return displaySettingsManager; }
	@Override public RotationLock rotationLock() { return rotationLock; }
	@Override public UserAgent userAgent() { return userAgent; }
	@Override public Device device() { return device; }
	@Override public AppOpen appOpen() { return appOpen; }
	@Override public WakeLockManager wakelocks() { return wakeLockManager; }
	@Override public BookmarkRepository bookmarks() { return bookmarkRepository; }

	/**
	 * Call when an Activity resumes or pauses.
	 *
	 * @param activity The activity that resumed, or null if pausing.
	 */
	public static void onActivityChange(AbsPocketActivity activity){
		sActivityContext = activity;
		if (activity != null) {
			setUserPresent(true, activity);
		}

		if (activity != null) {
			getApp().dispatcher().dispatch((component) -> component.onActivityResumed(activity));
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		dispatcher().dispatch((component) -> component.onConfigurationChanged(newConfig));
	}

	public static App from(Context context) {
		return (App) context.getApplicationContext();
	}

	/**
	 * Convenience method for getting a string resource.
	 *
	 * @deprecated access from a context you get in a constructor or parameter, avoid static access
	 */
	@Deprecated
	public static String getStringResource(int id) {
		if (id == 0) {
			return null;
		}

		return sContext.getString(id);
	}

	/**
	 * Returns the Application context
	 * @return
	 * @deprecated Avoid static access of {@link App} whenever possible. Instead pass component dependencies into classes. If static access is unavoidable, try to use {@link #from(Context)} instead. If in a screen, can use {@link AbsPocketActivity#app()} or {@link AbsPocketFragment#app()}
	 */
	@Deprecated
	public static App getApp(){
		return sContext;
	}

	/**
	 * Returns the currently focused Pocket Activity's context if there is one.
	 * @return
	 */
	public static AbsPocketActivity getActivityContext() {
		return sActivityContext;
	}

	/**
	 * Flag whether or not there is a Pocket Activity currently on screen and in focus of the user.
	 */
	public static void setUserPresent(boolean present, AbsPocketActivity activity) {
		sIsUserPresent = present;

		for (OnUserPresenceChangedListener listener : sOnUserPresenceChangedListeners) {
			listener.onUserPresenceChanged(present);
		}
		if (present) {
			getApp().dispatcher().dispatch((component) -> component.onUserPresent());
		} else {
			getApp().dispatcher().dispatch((component) -> component.onUserGone(activity));
		}
	}

	public static void addOnUserPresenceChangedListener(OnUserPresenceChangedListener listener) {
		sOnUserPresenceChangedListeners.add(listener);
	}

	public static void removeOnUserPresenceChangedListener(OnUserPresenceChangedListener listener) {
		sOnUserPresenceChangedListeners.remove(listener);
	}

	public interface OnUserPresenceChangedListener {
		void onUserPresenceChanged(boolean isInApp);
	}

	/**
	 * Convenience method for opening a url in another app.
	 *
	 * @param context context to start the other app with
	 * @param url url to open
	 * @param showDialogOnFail true to show a standard error dialog if no browser app available, false to fail silently.
	 * @return true if the browser activity was started, false if no app available to handle.
	 */
	public static boolean viewUrl(Context context, String url, boolean showDialogOnFail) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		if (!(context instanceof Activity)) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		if (IntentUtils.isActivityIntentAvailable(context, intent)) {
			context.startActivity(intent);
			return true;

		} else if (showDialogOnFail) {
			new AlertDialog.Builder(context)
				.setTitle(R.string.dg_browser_not_found_t)
				.setMessage(R.string.dg_browser_not_found_m)
				.setNeutralButton(R.string.ac_ok, null)
					.show();
			return false;
		} else {
			return false;
		}
	}

	/**
	 * Convenience for {@link #viewUrl(Context, String, boolean)} with showDialogOnFail = true;
	 *
	 * @param context context to start the other app with
	 * @param url url to open
	 * @return true if the browser activity was started, false if no app available to handle.
	 */
	public static boolean viewUrl(Context context, String url) {
		return viewUrl(context, url, true);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		dispatcher().dispatch((component) -> component.onLowMemory());
	}

	/**
	 * REVIEW duplicate of {@link #getActivityContext()} ?
	 * @return
	 */
	public static boolean isUserPresent() {
		return sActivityContext != null;
	}

}
