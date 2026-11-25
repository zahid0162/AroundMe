package com.zahid.aroundme.core.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object AndroidPlacesConfigProvider {
    fun load(context: Context): PlatformPlacesConfig {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        val meta = appInfo.metaData
        val placesKey = meta?.getString("com.zahid.aroundme.PLACES_API_KEY").orEmpty().trim()
        val mapsKey = meta?.getString("com.google.android.geo.API_KEY").orEmpty().trim()
        val proxyUrl = meta?.getString("com.zahid.aroundme.PLACES_PROXY_BASE_URL").orEmpty().trim()

        val resolvedKey = placesKey.ifEmpty { mapsKey }
        val headers = buildMap {
            put("X-Android-Package", context.packageName)
            resolveSigningCertSha1(context)?.let { sha1 ->
                put("X-Android-Cert", sha1)
            }
        }

        return PlatformPlacesConfig(
            apiKey = resolvedKey,
            proxyBaseUrl = proxyUrl,
            platformHeaders = headers
        )
    }

    @Suppress("DEPRECATION")
    private fun resolveSigningCertSha1(context: Context): String? {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pkgInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                pkgInfo.signingInfo?.apkContentsSigners
            } else {
                val pkgInfo = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                pkgInfo.signatures
            }
            val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return null
            val digest = MessageDigest.getInstance("SHA-1").digest(certBytes)
            digest.joinToString("") { byte ->
                "%02X".format(byte)
            }
        } catch (_: Exception) {
            null
        }
    }
}
