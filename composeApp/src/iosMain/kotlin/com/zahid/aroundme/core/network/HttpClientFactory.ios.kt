package com.zahid.aroundme.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createAroundMeHttpClient(): HttpClient = HttpClient(Darwin) {
    configureAroundMeClient()
}
