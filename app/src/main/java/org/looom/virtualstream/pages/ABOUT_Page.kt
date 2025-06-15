package org.looom.virtualstream.pages

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AboutPage(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(), // ✅ 占满父容器
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                overScrollMode = WebView.OVER_SCROLL_NEVER // 禁止过度滚动回弹效果
                setBackgroundColor(Color.TRANSPARENT) // 背景透明，避免白边

                // 让链接在浏览器中打开
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest
                    ): Boolean {
                        val url = request.url.toString()
                        if (url.startsWith("http") || url.startsWith("https")) {
                            val intent = Intent(Intent.ACTION_VIEW, request.url)
                            context.startActivity(intent)
                            return true
                        }
                        return false
                    }
                }

                loadUrl("file:///android_asset/about.html")
            }

        }
    )
}