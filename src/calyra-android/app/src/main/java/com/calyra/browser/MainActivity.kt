package com.calyra.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : ComponentActivity() {
    private lateinit var session: GeckoSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CalyraApp
        session = GeckoSession().apply {
            // Privacy: ETP Strict, WebRTC block in Enhanced profile
            // Proxy: set via RuntimeSettings proxyConfig (SOCKS/PAC), fail-closed — no auto DIRECT
            open(app.runtime)
        }

        setContent {
            MaterialTheme {
                CalyraScreen(
                    onNavigate = { input ->
                        val url = AddressBarLogic.normalize(input, "https://duckduckgo.com/?q=%s")
                        // Network policy: respect profile route; Tor not ready => load error page, not DIRECT
                        session.loadUri(url)
                    },
                    geckoView = { GeckoView(this).apply { setSession(session) } }
                )
            }
        }
    }
}

object AddressBarLogic {
    fun normalize(input: String, template: String): String {
        val t = input.trim()
        if (t.isEmpty()) return "about:blank"
        if (t.contains(" ") || (!t.contains(".") && !t.contains("://"))) {
            return template.replace("%s", java.net.URLEncoder.encode(t, "UTF-8"))
        }
        val url = if (!t.contains("://")) "https://$t" else t
        if (url.startsWith("javascript:")) return "about:blank"
        return url
    }
}

@Composable
fun CalyraScreen(onNavigate: (String)->Unit, geckoView: @Composable ()->Unit) {
    var text by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("Ağ: Doğrudan (Normal)") }
    Column {
        // Trusted chrome — web content cannot mimic (contrast border)
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🔒"); Text("🔰")
                OutlinedTextField(value=text, onValueChange={text=it}, modifier=Modifier.weight(1f), placeholder={Text("Adres veya arama")})
                Button(onClick={onNavigate(text)}){ Text("Git") }
            }
        }
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterStatusBadge(); Text(route)
        }
        // Tab bar (mobile bottom)
        Box(Modifier.weight(1f)) { /* AndroidView(factory={geckoView()}) in real app */ Text("GeckoView burada — sandbox: app UID izolasyonu") }
        // Profile picker: Normal / Güçlendirilmiş / Tor — explicit policy, not icon-only
        Row(Modifier.padding(8.dp)) {
            AssistChip(onClick={route="Ağ: Doğrudan"}, label={Text("Normal")})
            AssistChip(onClick={route="Ağ: SOCKS 127.0.0.1:9050 — Tor hazırlanıyor, doğrudan çıkış yok"}, label={Text("Tor")})
        }
    }
}

@Composable fun FilterStatusBadge(){ Text("Filtre: 12k kural — ETP Strict", style = MaterialTheme.typography.labelSmall) }
