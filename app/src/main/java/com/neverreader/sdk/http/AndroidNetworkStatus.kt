package com.neverreader.sdk.http

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.neverreader.util.android.ApiLevel
import com.neverreader.util.java.Safe

/**
 * A [NetworkStatus] implementation using Android's [ConnectivityManager]'s listeners.
 * There is slightly more fine tuned accuracy on api levels 23+ and 24+ as some other apis become available.
 */
class AndroidNetworkStatus(context: Context) : NetworkStatus {
    private val manager: ConnectivityManager
    private val mOnline: MutableSet<Network?> = HashSet<Network?>()
    private val mWifi: MutableSet<Network?> = HashSet<Network?>()
    private val listeners: MutableSet<NetworkStatus.Listener> = HashSet<NetworkStatus.Listener>()
    private var mIsOnline = false
    private var mIsWifi = false
    private var mIsUnmetered = false
    private var active: Network? = null
    private var lastDisconnect: Long = 0

    init {
        manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (manager == null) throw AssertionError("Couldn't get ConnectivityManager")

        for (n in manager.getAllNetworks()) {
            val c = manager.getNetworkCapabilities(n)
            if (c == null) continue
            if (c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && (ApiLevel.isPreP() || c.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND))
                && (c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            ) {
                setOnline(n)
            }
        }
        updateStatus()

        val b = NetworkRequest.Builder()
        b.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        b.addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        manager.registerNetworkCallback(b.build(), object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                setOnline(network)
                updateStatus()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                updateCapabilities(network, networkCapabilities)
                updateStatus()
            }

            override fun onLost(network: Network) {
                lost(network)
                updateStatus()
            }
        })
        if (ApiLevel.isNougatOrGreater()) {
            manager.registerDefaultNetworkCallback(object : NetworkCallback() {
                override fun onAvailable(network: Network) {
                    active = network
                    updateStatus()
                }

                override fun onLost(network: Network) {
                    if (network == active) active = null
                    updateStatus()
                }
            })
        }
    }

    @Synchronized
    private fun setOnline(network: Network?) {
        mOnline.add(network)
        updateCapabilities(network, manager.getNetworkCapabilities(network))
    }

    @Synchronized
    private fun updateCapabilities(network: Network?, nc: NetworkCapabilities?) {
        mWifi.remove(network)
        if (nc == null) return
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            mWifi.add(network)
        }
    }

    @Synchronized
    private fun lost(network: Network?) {
        mOnline.remove(network)
        mWifi.remove(network)
        if (mOnline.isEmpty()) {
            lastDisconnect = System.currentTimeMillis()
        }
    }

    @Synchronized
    private fun updateStatus() {
        val newOnline = mOnline.isEmpty() == false
        val newWifi: Boolean
        val newUnmetered: Boolean
        if (newOnline) {
            if (active != null) {
                newWifi = mWifi.contains(active)
            } else {
                // We don't know which one is active, so have to fall back to the older method
                newWifi = Safe.getBoolean(Safe.Get {
                    manager.getActiveNetworkInfo()!!.getType() == ConnectivityManager.TYPE_WIFI
                })
            }
            newUnmetered = !manager.isActiveNetworkMetered()
        } else {
            newWifi = false
            newUnmetered = false
        }

        val changed =
            newOnline != mIsOnline || newWifi != mIsWifi || newUnmetered != mIsUnmetered

        mIsOnline = newOnline
        mIsWifi = newWifi
        mIsUnmetered = newUnmetered

        if (changed) {
            for (listener in ArrayList<NetworkStatus.Listener>(listeners)) {
                listener.onStatusChanged(this)
            }
        }
    }

    override val isOnline: Boolean
        @Synchronized get() = mIsOnline

    @Synchronized
    override fun isStable(millis: Long): Boolean {
        if (!mIsOnline) return false
        return lastDisconnect == 0L || lastDisconnect < System.currentTimeMillis() - millis
    }

    override val isWifi: Boolean
        @Synchronized get() = mIsWifi

    override val isUnmetered: Boolean
        @Synchronized get() = mIsUnmetered

    @Synchronized
    override fun addListener(listener: NetworkStatus.Listener?) {
        listeners.add(listener!!)
    }

    @Synchronized
    override fun removeListener(listener: NetworkStatus.Listener?) {
        listeners.remove(listener)
    }
}
