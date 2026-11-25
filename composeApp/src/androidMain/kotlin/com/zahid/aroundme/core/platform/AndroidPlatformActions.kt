package com.zahid.aroundme.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.zahid.aroundme.domain.model.GeoPoint
import java.net.URLEncoder

class AndroidPlatformActions(
    private val context: Context
) : PlatformActions {

    override fun openDirections(
        placeName: String,
        location: GeoPoint,
        placeId: String,
        directionsUri: String?
    ) {
        val targetUrl = if (isSafeExternalUrl(directionsUri)) {
            directionsUri!!
        } else {
            val encodedPlaceId = URLEncoder.encode(placeId, "UTF-8")
            "https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}&destination_place_id=$encodedPlaceId"
        }
        launchViewUri(Uri.parse(targetUrl))
    }

    override fun dialPhone(phoneNumber: String) {
        val sanitized = phoneNumber.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (sanitized.isEmpty()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    override fun openWebsite(url: String) {
        if (!isSafeExternalUrl(url)) return
        launchViewUri(Uri.parse(url.trim()))
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
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, placeName)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(intent, placeName).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
    }

    private fun launchViewUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    private fun isSafeExternalUrl(url: String?): Boolean {
        val trimmed = url?.trim() ?: return false
        return trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true)
    }
}
