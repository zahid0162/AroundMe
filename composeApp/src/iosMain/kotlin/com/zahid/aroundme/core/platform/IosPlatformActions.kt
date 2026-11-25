package com.zahid.aroundme.core.platform

import com.zahid.aroundme.domain.model.GeoPoint
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

@OptIn(ExperimentalForeignApi::class)
class IosPlatformActions : PlatformActions {

    override fun openDirections(
        placeName: String,
        location: GeoPoint,
        placeId: String,
        directionsUri: String?
    ) {
        val targetUrl = if (isSafeExternalUrl(directionsUri)) {
            directionsUri!!
        } else {
            val encodedId = encodeQueryComponent(placeId)
            "https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}&destination_place_id=$encodedId"
        }
        openExternalUrl(targetUrl)
    }

    override fun dialPhone(phoneNumber: String) {
        val sanitized = phoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (sanitized.isEmpty()) return
        openExternalUrl("tel:$sanitized")
    }

    override fun openWebsite(url: String) {
        if (!isSafeExternalUrl(url)) return
        openExternalUrl(url.trim())
    }

    override fun sharePlace(placeName: String, address: String, googleMapsUri: String?) {
        val shareText = buildString {
            append(placeName)
            if (address.isNotBlank()) {
                append("\n")
                append(address)
            }
            if (isSafeExternalUrl(googleMapsUri)) {
                append("\n")
                append(googleMapsUri)
            }
        }
        val controller = UIActivityViewController(
            activityItems = listOf(shareText),
            applicationActivities = null
        )
        topViewController()?.presentViewController(controller, animated = true, completion = null)
    }

    private fun openExternalUrl(rawUrl: String) {
        val url = NSURL.URLWithString(rawUrl) ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
    }

    private fun isSafeExternalUrl(url: String?): Boolean {
        val trimmed = url?.trim() ?: return false
        return trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun encodeQueryComponent(value: String): String {
        val ns = value as NSString
        return ns.stringByAddingPercentEncodingWithAllowedCharacters(
            NSCharacterSet.URLQueryAllowedCharacterSet
        ) ?: value
    }

    @Suppress("DEPRECATION")
    private fun topViewController(): UIViewController? {
        val windows = UIApplication.sharedApplication.windows
        val keyWindow = windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
            ?: UIApplication.sharedApplication.keyWindow
        var top = keyWindow?.rootViewController
        while (top?.presentedViewController != null) {
            top = top.presentedViewController
        }
        return top
    }
}
