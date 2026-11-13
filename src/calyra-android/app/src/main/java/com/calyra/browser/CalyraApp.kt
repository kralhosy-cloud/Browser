package com.calyra.browser

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime

class CalyraApp : Application() {
    lateinit var runtime: GeckoRuntime
    override fun onCreate() {
        super.onCreate()
        val settings = GeckoRuntimeSettingsBuilder().trackingProtectionEnabled(true).build()
        runtime = GeckoRuntime.create(this, settings)
    }
}
