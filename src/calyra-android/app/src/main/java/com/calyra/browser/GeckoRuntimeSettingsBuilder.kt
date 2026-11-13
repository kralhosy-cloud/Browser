package com.calyra.browser

import org.mozilla.geckoview.GeckoRuntimeSettings

class GeckoRuntimeSettingsBuilder {
    private var tp = true
    fun trackingProtectionEnabled(v: Boolean) = apply { tp = v }
    fun build(): GeckoRuntimeSettings = GeckoRuntimeSettings.Builder()
        .enhancedTrackingProtection(GeckoRuntimeSettings.TrackingProtection.ENABLED)
        .contentBlocking(GeckoRuntimeSettings.ContentBlocking.EtpStrict())
        .build()
}
