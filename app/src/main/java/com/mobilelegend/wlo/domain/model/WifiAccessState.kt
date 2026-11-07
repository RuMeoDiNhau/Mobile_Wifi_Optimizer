package com.mobilelegend.wlo.domain.model

/** Permissions for the APIs actually used: reading Wi-Fi identity and device location.
 * Nearby Wi-Fi is for connection-management APIs; it does not replace location here.
 */
data class WifiAccessState(
    val sdkInt: Int = 34,
    val fineLocation: Boolean = false,
    val coarseLocation: Boolean = false,
    val locationServiceEnabled: Boolean = false
) {
    val canUseMap: Boolean get() = fineLocation || coarseLocation
    val canReadWifiIdentity: Boolean get() = if (sdkInt >= 29) fineLocation else canUseMap
    val locationServicesReady: Boolean get() = sdkInt < 28 || locationServiceEnabled
}
