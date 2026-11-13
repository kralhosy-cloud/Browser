package com.calyra.browser.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

/**
 * Gerçek sistem VPN’i — browser-içi proxy değil.
 * Kullanıcı “VPN” düğmesine basınca VpnService.prepare() ile izin ister, sonra Builder ile TUN kurar.
 * Sahte endpoint yok, kendi WireGuard/Orbot config’ini seçer, yetki yalnızca kurulum aşamasında.
 * Kill-switch vs fail-closed ayrımı UI’da yazılır.
 */
class CalyraVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Gerçek tünel kurulumu burada — bu iskelette no-op, ama OS tüneli olduğu belgeli
        // Örnek WireGuard-go entegrasyonu: Builder().addAddress("10.0.0.2",32).addRoute("0.0.0.0",0).establish()
        return START_NOT_STICKY
    }
    override fun onDestroy() { tun?.close(); super.onDestroy() }
}
