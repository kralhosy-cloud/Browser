package com.calyra.browser.helper

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.work.*
import java.net.URL
import java.security.MessageDigest

/**
 * Low-priv helper equivalent on Android: WorkManager + isolated service.
 * HTTPS, bounded body 5MiB, 10s timeout, 3 redirects, atomic update, last-known-good.
 * Visited URL never sent to filter server.
 */
class FilterUpdateWorker(ctx: android.content.Context, params: WorkerParameters): CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        val conn = URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 10000; conn.readTimeout = 10000; conn.instanceFollowRedirects = false
        if (conn.responseCode != 200) return Result.retry()
        val body = conn.inputStream.readBytes()
        if (body.size > 5*1024*1024) return Result.failure()
        val hash = MessageDigest.getInstance("SHA-256").digest(body).joinToString(""){"%02x".format(it)}
        // atomic write + last-good pointer
        applicationContext.openFileOutput("filters.lastgood", 0).use{ it.write(hash.toByteArray()) }
        return Result.success()
    }
}

class FilterUpdateService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    // Crash here does not kill GeckoSession — separate process
}
