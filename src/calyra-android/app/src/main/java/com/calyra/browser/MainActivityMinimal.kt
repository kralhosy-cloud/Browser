package com.calyra.browser
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.Button
import android.view.ViewGroup

class MainActivityMinimal : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val input = EditText(this).apply { hint = "https://example.com"; layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
        val btn = Button(this).apply { text = "Git" }
        val wv = WebView(this).apply { webViewClient = WebViewClient(); settings.javaScriptEnabled = true }
        btn.setOnClickListener {
            var url = input.text.toString().trim()
            if (!url.startsWith("http")) url = "https://$url"
            wv.loadUrl(url)
        }
        bar.addView(input); bar.addView(btn)
        layout.addView(bar); layout.addView(wv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(layout)
        wv.loadUrl("https://example.com")
    }
}
