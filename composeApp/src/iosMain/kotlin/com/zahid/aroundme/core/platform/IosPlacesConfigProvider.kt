package com.zahid.aroundme.core.platform

import platform.Foundation.NSBundle

object IosPlacesConfigProvider {
    fun load(): PlatformPlacesConfig {
        val bundle = NSBundle.mainBundle
        val placesKey = readPlistString(bundle, "PlacesApiKey")
        val gmsKey = readPlistString(bundle, "GMSApiKey")
        val proxyUrl = readPlistString(bundle, "PlacesProxyBaseUrl")

        val resolvedKey = placesKey.ifEmpty { gmsKey }
        val headers = buildMap {
            bundle.bundleIdentifier?.takeIf { it.isNotBlank() }?.let { bundleId ->
                put("X-Ios-Bundle-Identifier", bundleId)
            }
        }

        return PlatformPlacesConfig(
            apiKey = resolvedKey,
            proxyBaseUrl = proxyUrl,
            platformHeaders = headers
        )
    }

    private fun readPlistString(bundle: NSBundle, key: String): String {
        val raw = (bundle.objectForInfoDictionaryKey(key) as? String)?.trim().orEmpty()
        // Ignore unresolved Xcode build variables like $(PLACES_API_KEY)
        return if (raw.startsWith("$(") && raw.endsWith(")")) "" else raw
    }
}
