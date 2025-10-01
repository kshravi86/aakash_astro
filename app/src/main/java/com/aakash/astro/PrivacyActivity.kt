package com.aakash.astro

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class PrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wv = WebView(this)
        setContentView(wv)
        wv.settings.javaScriptEnabled = false
        wv.loadUrl("file:///android_asset/privacy_policy.html")
    }
}

