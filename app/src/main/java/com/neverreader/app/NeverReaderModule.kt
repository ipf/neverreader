package com.neverreader.app

import android.content.Context
import android.preference.PreferenceManager
import com.neverreader.repository.ItemRepository
import com.neverreader.repository.NeverReaderItemRepository
import com.neverreader.repository.NeverReaderUserRepository
import com.neverreader.repository.UserRepository
import com.neverreader.sdk.http.AndroidNetworkStatus
import com.neverreader.sdk.http.NetworkStatus
import com.neverreader.util.DrawableLoader
import com.neverreader.util.StringLoader
import com.neverreader.util.prefs.AndroidPrefStore
import com.neverreader.util.prefs.Preferences
import com.neverreader.util.prefs.Prefs
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The app's Hilt-provided components.
 */
@Module
@InstallIn(SingletonComponent::class)
class NeverReaderModule {

    @Provides @Singleton
    fun providePrefs(@ApplicationContext context: Context): Preferences =
        Prefs(
            AndroidPrefStore(PreferenceManager.getDefaultSharedPreferences(context)),
            AndroidPrefStore(context.getSharedPreferences("neverreaderPrefs", 0))
        )

    @Provides @Singleton
    fun provideAppMode(): AppMode = if (BuildConfig.DEBUG) AppMode.DEV else AppMode.PRODUCTION

    @Provides @Singleton
    fun provideNetworkStatus(@ApplicationContext context: Context): NetworkStatus =
        AndroidNetworkStatus(context)

    @Provides @Singleton
    fun provideStringLoader(@ApplicationContext context: Context): StringLoader = StringLoader(context)

    @Provides @Singleton
    fun provideNeverReaderDatabase(@ApplicationContext context: Context): com.neverreader.backend.db.NeverReaderDatabase =
        com.neverreader.backend.DataGraph.database(context)

    @Provides @Singleton
    fun provideDrawableLoader(@ApplicationContext context: Context): DrawableLoader = DrawableLoader(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NeverReaderInterfaces {
    @Binds @Singleton
    abstract fun itemRepository(impl: NeverReaderItemRepository): ItemRepository

    @Binds @Singleton
    abstract fun userRepository(impl: NeverReaderUserRepository): UserRepository
}
