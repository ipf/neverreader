package com.neverreader.sdk.http

/**
 * Information about the device's internet connection.
 */
interface NetworkStatus {
    /**
     * @return true if the device has an active internet connection.
     * Implementations will attempt their best to have this mean that there is an actual available connection to the internet, not just connected to a network.
     * However some device's may or may not support that level of correctness. At the very least this indicates connected to some network.
     */
    val isOnline: Boolean

    /**
     * @return true if the connection has been online for at least the X milliseconds with no known disconnections or loss in service.
     * If the provided duration is longer than the total known time, it will use the total known time instead of your provided duration.
     */
    fun isStable(duration: Long): Boolean

    /**
     * @return true if the active network is a wifi network.
     */
    val isWifi: Boolean

    /**
     * @return true if the active network is unmetered.
     */
    val isUnmetered: Boolean

    fun addListener(listener: Listener?)
    fun removeListener(listener: Listener?)

    interface Listener {
        /** Some property of the network connection has changed (online, wifi or unmetered). Requery as needed. Warning: This could be invoked from a background thread.  */
        fun onStatusChanged(status: NetworkStatus?)
    }
}
