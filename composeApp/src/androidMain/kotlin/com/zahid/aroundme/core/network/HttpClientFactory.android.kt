package com.zahid.aroundme.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createAroundMeHttpClient(): HttpClient = HttpClient(OkHttp) {
    configureAroundMeClient()
}
