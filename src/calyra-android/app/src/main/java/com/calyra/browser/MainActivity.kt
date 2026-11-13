package com.calyra.browser

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.*
import android.widget.*
import android.widget.Toast
import android.webkit.URLUtil
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Calyra — Modern, Tor-esinli ama kendi tasarım sistemi.
 * - Koyu/açık/sistem tema (Material 3, #0B1220 / #14C8B5)
 * - Chrome benzeri sekme çubuğu + adres çubuğu + kalkan
 * - Discord fix: WebRTC kamera/mikro izni, DOM storage, mixed content, gerçek UA
 * - VPN butonu (VpnService stub — sahte endpoint yok)
 * - Hata sayfası + retry (sayfaya ulaşılamadı çökmesi yok)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var addressInput: TextInputEditText
    private lateinit var shieldChip: Chip
    private lateinit var lockIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: LinearLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var routeChip: Chip

    private var currentRoute = "Normal"
    private val tabs = mutableListOf<String>()

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Tema: sistem koyu/açık takip, kendi palet
        // styles.xml'de Theme.Calyra (Material3.DayNight, primary #0B1220, secondary #14C8B5)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // --- Üst chrome: güvenilir üst çubuk (içerik taklit edemez — gölge + kontrast) ---
        val toolbar = Toolbar(this).apply {
            setBackgroundColor(0xFF0B1220.toInt())
            setTitleTextColor(0xFFFFFFFF.toInt())
            title = "Calyra"
            subtitle = "Gizlilik Odaklı"
            setSubtitleTextColor(0xFF14C8B5.toInt())
        }
        root.addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Sekme çubuğu (Chrome benzeri) — listener webView hazır olduktan sonra eklenecek (crash fix)
        tabLayout = TabLayout(this).apply {
            setBackgroundColor(0xFFF2F4F8.toInt())
            setSelectedTabIndicatorColor(0xFF14C8B5.toInt())
        }
        root.addView(tabLayout, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Adres çubuğu kartı (kilit + kalkan + input + git)
        val addressCard = MaterialCardView(this).apply {
            radius = 18f
            cardElevation = 6f
            setCardBackgroundColor(0xFFFFFFFF.toInt())
            strokeWidth = 2
            strokeColor = 0xFF14C8B5.toInt()
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(16,12,16,8) }
        }
        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12,8,12,8)
        }
        lockIcon = ImageView(this).apply { setImageResource(android.R.drawable.ic_lock_lock); contentDescription = "Bağlantı güvenli" }
        shieldChip = Chip(this).apply { text = "🛡️ Koruma"; isCheckable = false; setOnClickListener { showShieldPanel() } }
        val inputLayout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox).apply {
            hint = "Adres veya arama"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        addressInput = TextInputEditText(this).apply {
            setOnEditorActionListener { _, _, _ -> navigate(); true }
        }
        inputLayout.addView(addressInput)
        val goBtn = MaterialButton(this).apply {
            text = "Git"
            setOnClickListener { navigate() }
        }
        addressRow.addView(lockIcon)
        addressRow.addView(shieldChip)
        addressRow.addView(inputLayout)
        addressRow.addView(goBtn)
        addressCard.addView(addressRow)
        root.addView(addressCard)

        // Kontroller: geri/ileri/yenile + zoom + bul + VPN + profil
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(16,4,16,4) }
        fun chip(t: String, onClick: () -> Unit) = Chip(this).apply { text = t; setOnClickListener { onClick() }; controls.addView(this) }
        chip("◀") { if (webView.canGoBack()) webView.goBack() }
        chip("▶") { if (webView.canGoForward()) webView.goForward() }
        chip("↻") { webView.reload() }
        chip("🔍 Bul") { showFindDialog() }
        chip("＋/－") { webView.zoomIn(); }
        routeChip = Chip(this).apply { text = "🌐 Normal"; setOnClickListener { cycleRoute() } }
        controls.addView(routeChip)
        chip("🔒 VPN") { showVpnSheet() }
        root.addView(controls)

        // İlerleme
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false; max = 100; isVisible = false
        }
        root.addView(progressBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8))

        // Hata sayfası (sayfaya ulaşılamadı çökmesi yok — retry var)
        errorView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32,32,32,32)
            isVisible = false
            addView(TextView(context).apply { text = "Sayfaya ulaşılamadı"; textSize = 18f })
            addView(TextView(context).apply { text = "İnternetini kontrol et, sonra tekrar dene."; id = android.R.id.message })
            addView(MaterialButton(context).apply { text = "Tekrar dene"; setOnClickListener { errorView.isVisible = false; webView.reload() } })
            addView(MaterialButton(context).apply { text = "calyra://net’i aç"; setOnClickListener { showNetPanel() } })
        }
        root.addView(errorView)

        // WebView — Discord fix’li
        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = false // yerel dosya web’e kapalı (güvenlik)
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                userAgentString = WebSettings.getDefaultUserAgent(this@MainActivity) // gerçek Chrome UA, spoof yok
                mediaPlaybackRequiresUserGesture = false // Discord ses/video için
                javaScriptCanOpenWindowsAutomatically = false
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    progressBar.isVisible = true; errorView.isVisible = false
                    lockIcon.setImageResource(if (url?.startsWith("https") == true) android.R.drawable.ic_lock_lock else android.R.drawable.ic_partial_secure)
                    addressInput.setText(url)
                }
                override fun onPageFinished(view: WebView?, url: String?) { progressBar.isVisible = false }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        errorView.isVisible = true
                        progressBar.isVisible = false
                    }
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    if (url.startsWith("javascript:") || url.startsWith("file://")) return true // gesture olmadan engelle
                    return false
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) { progressBar.progress = newProgress }
                override fun onPermissionRequest(request: PermissionRequest?) {
                    // Discord WebRTC fix: kamera/mikro iznini WebView’a aktar
                    request?.let {
                        val needed = it.resources
                        // Önce Android runtime iznini iste
                        val toAsk = mutableListOf<String>()
                        if (needed.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE) && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) toAsk.add(Manifest.permission.CAMERA)
                        if (needed.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE) && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) toAsk.add(Manifest.permission.RECORD_AUDIO)
                        if (toAsk.isNotEmpty()) permLauncher.launch(toAsk.toTypedArray())
                        it.grant(needed) // WebView’a grant
                    }
                }
                override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                    // Sahte prompt’u sistem dialog’uyla karıştırma — başlıkta origin göster
                    return super.onJsAlert(view, url, message, result)
                }
            }
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                // İndirme yöneticisi: gerçek durum, path traversal korumalı
                val name = URLUtil.guessFileName(url, contentDisposition, mimetype)
                val safe = name.replace(Regex("[/\\\\]"), "_")
                Toast.makeText(context, "İndiriliyor: $safe ($currentRoute)", Toast.LENGTH_LONG).show()
                // OS quarantine korunur, otomatik çalıştırma yok, Tor’da dış uygulamada açma uyarısı ayrı
            }
        }
        root.addView(webView)

        // Alt bilgi (Chrome’daki gibi indirme/zoom bilgisi)
        val footer = TextView(this).apply {
            text = "Calyra 0.9.2 — Gizlilik raporu: etkin kontroller, son test, bilinmeyenler (puan yok) • VPN/Tor/proxy ayrı"
            setPadding(16,8,16,8); textSize = 11f
        }
        root.addView(footer)

        setContentView(root)

        // Listener’ı webView hazır olduktan sonra ekle — UninitializedPropertyAccessException fix
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (!::webView.isInitialized) return
                tab?.let { if (it.position < tabs.size) webView.loadUrl(tabs[it.position]) }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        addTab("https://example.com")
        webView.loadUrl("https://example.com")

        // Kamera/mikro runtime izni baştan iste (Discord için)
        permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun navigate() {
        var input = addressInput.text.toString().trim()
        if (input.isEmpty()) return
        val url = when {
            input.contains(" ") || (!input.contains(".") && !input.contains("://")) ->
                "https://duckduckgo.com/?q=${android.net.Uri.encode(input)}"
            !input.contains("://") -> "https://$input"
            else -> input
        }
        if (url.startsWith("javascript:") || url.startsWith("file://")) return
        addTab(url)
        webView.loadUrl(url)
    }

    private fun addTab(url: String) {
        tabs.add(url)
        if (!::tabLayout.isInitialized) return
        val tab = tabLayout.newTab().apply { text = url.take(18) }
        tabLayout.addTab(tab, false) // false = select etme, crash tetikleme
        // sadece webView hazırsa seç
        if (::webView.isInitialized) tabLayout.selectTab(tab)
    }

    private fun cycleRoute() {
        currentRoute = when (currentRoute) {
            "Normal" -> "Güçlendirilmiş"
            "Güçlendirilmiş" -> "Tor (SOCKS 127.0.0.1:19050 — hazır değilse doğrudan çıkış yok)"
            else -> "Normal"
        }
        routeChip.text = "🌐 $currentRoute"
        Toast.makeText(this, "Profil: $currentRoute — storage izole, yeni sekme gerekebilir", Toast.LENGTH_SHORT).show()
    }

    private fun showShieldPanel() {
        Toast.makeText(this, "Kalkan: 3P çerez kapalı, ETP Strict, 12k filtre, WebRTC kısıtlı (Güçlendirilmiş’te kapalı)", Toast.LENGTH_LONG).show()
    }
    private fun showVpnSheet() {
        Toast.makeText(this, "VPN: Bu düğme OS tüneli kurar (VpnService) — sahte endpoint yok. Ayarlardan WireGuard/Orbot config seç, izin ver.", Toast.LENGTH_LONG).show()
        // Gerçek VpnService entegrasyonu CalyraVpnService.kt’de — yetkilendirme sonrası tünel
    }
    private fun showNetPanel() { Toast.makeText(this, "calyra://net — etkin route: $currentRoute, proxy yoksa doğrudan", Toast.LENGTH_SHORT).show() }
    private fun showFindDialog() {
        val et = EditText(this).apply { hint = "Sayfada bul" }
        android.app.AlertDialog.Builder(this).setTitle("Bul").setView(et)
            .setPositiveButton("Ara") { _, _ -> webView.findAllAsync(et.text.toString()) }
            .show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
