package com.zahid.aroundme

import androidx.compose.ui.window.ComposeUIViewController
import com.zahid.aroundme.di.initKoin
import platform.UIKit.UIViewController

private var koinInitialized = false

fun MainViewController(): UIViewController {
    if (!koinInitialized) {
        initKoin()
        koinInitialized = true
    }
    return ComposeUIViewController { AroundMeApp() }
}
