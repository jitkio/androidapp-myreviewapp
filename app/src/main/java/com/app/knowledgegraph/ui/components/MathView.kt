package com.app.knowledgegraph.ui.components

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.util.Log
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "MathView"

// KaTeX CDN sources - try China-accessible CDNs first
private const val KATEX_CSS = "https://unpkg.com/katex@0.16.9/dist/katex.min.css"
private const val KATEX_JS = "https://unpkg.com/katex@0.16.9/dist/katex.min.js"
private const val KATEX_AUTO = "https://unpkg.com/katex@0.16.9/dist/contrib/auto-render.min.js"

// LaTeX 公式正则：匹配 $...$ 或 $$...$$ 中有实际内容的
private val MATH_REGEX = Regex("""\$\$[^$]+\$\$|\$[^$]+\$""")

/**
 * 轻量级纯文本版本，用于列表项。
 * 剥离 LaTeX 标记，只显示纯文本预览，避免在 LazyColumn 中创建 WebView。
 */
@Composable
fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 3
) {
    if (text.isBlank()) return

    val plainText = remember(text) {
        stripLatex(text)
    }

    Text(
        text = plainText,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * 完整版 MathView（带 WebView + KaTeX 渲染），只在详情页使用。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MathView(
    text: String,
    modifier: Modifier = Modifier,
    figureSvg: String = "",
    baseFontSize: Int = 16
) {
    if (text.isBlank() && figureSvg.isBlank()) return

    val hasMath = remember(text) { MATH_REGEX.containsMatchIn(text) }
    val hasSvg = figureSvg.isNotBlank()

    if (!hasMath && !hasSvg) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = modifier)
        return
    }

    val fullHtml = remember(text, figureSvg, baseFontSize) {
        buildFullHtml(text, figureSvg, baseFontSize)
    }

    var contentHeightDp by remember { mutableIntStateOf(0) }

    val heightModifier = if (contentHeightDp > 0) {
        Modifier.height(contentHeightDp.dp)
    } else {
        Modifier.defaultMinSize(minHeight = 48.dp)
    }

    AndroidView(
        modifier = modifier.fillMaxWidth().then(heightModifier),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = false
                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                settings.blockNetworkImage = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun reportHeight(cssPixels: Float) {
                        post {
                            val dp = cssPixels.toInt() + 8
                            if (dp > 0 && dp != contentHeightDp) {
                                contentHeightDp = dp
                            }
                        }
                    }
                }, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d(TAG, "Page loaded OK")
                        view?.postDelayed({
                            view.evaluateJavascript(
                                "(function(){ AndroidBridge.reportHeight(document.body.scrollHeight); })()",
                                null
                            )
                        }, 150)
                    }
                }
                tag = fullHtml
                loadDataWithBaseURL("https://unpkg.com", fullHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // 只在 HTML 内容变化时才重新加载，避免无意义的重复渲染
            if (webView.tag != fullHtml) {
                webView.tag = fullHtml
                webView.loadDataWithBaseURL("https://unpkg.com", fullHtml, "text/html", "UTF-8", null)
            }
        },
        onRelease = { webView ->
            // 释放 WebView 资源，防止内存泄漏
            webView.stopLoading()
            webView.removeJavascriptInterface("AndroidBridge")
            webView.destroy()
        }
    )
}

/**
 * 剥离 LaTeX 标记，返回可读的纯文本。
 */
private fun stripLatex(text: String): String {
    return text
        .replace(Regex("""\$\$([^$]+)\$\$""")) { it.groupValues[1] }
        .replace(Regex("""\$([^$]+)\$""")) { it.groupValues[1] }
        .replace("\\frac{", "(")
        .replace("}{", ")/(")
        .replace("\\sqrt{", "sqrt(")
        .replace("\\", "")
        .replace("{", "")
        .replace("}", ")")
        .replace("  ", " ")
        .trim()
}

private fun buildFullHtml(text: String, figureSvg: String, fontSize: Int): String {
    val d = "\u0024"
    val escapedContent = escapeHtmlPreservingMath(text)
    val svgBlock = if (figureSvg.isNotBlank()) "<div style='text-align:center;margin:12px 0'>" + figureSvg + "</div>" else ""
    return "<!DOCTYPE html><html><head>" +
        "<meta charset='utf-8'>" +
        "<meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>" +
        "<style>" +
        "*{margin:0;padding:0;box-sizing:border-box}" +
        "body{font-family:-apple-system,system-ui,sans-serif;font-size:" + fontSize + "px;" +
        "line-height:1.7;color:#1a1a1a;padding:8px 2px;word-wrap:break-word;background:transparent}" +
        ".katex{font-size:1.1em}" +
        ".katex-display{margin:8px 0;overflow-x:auto;overflow-y:hidden}" +
        "</style>" +
        "<link rel='stylesheet' href='" + KATEX_CSS + "'>" +
        "</head><body>" +
        "<div id='content'>" + escapedContent + svgBlock + "</div>" +
        "<script>" +
        "function loadScript(src,cb){var s=document.createElement('script');s.src=src;s.onload=cb;s.onerror=function(){" +
        "var c2=src.replace('unpkg.com','cdn.jsdelivr.net/npm');" +
        "var s2=document.createElement('script');s2.src=c2;s2.onload=cb;document.head.appendChild(s2)" +
        "};document.head.appendChild(s)}" +
        "loadScript('" + KATEX_JS + "',function(){" +
        "loadScript('" + KATEX_AUTO + "',function(){" +
        "try{renderMathInElement(document.getElementById('content'),{" +
        "delimiters:[" +
        "{left:'" + d + d + "',right:'" + d + d + "',display:true}," +
        "{left:'" + d + "',right:'" + d + "',display:false}" +
        "],throwOnError:false})}catch(e){}" +
        "setTimeout(function(){if(typeof AndroidBridge!=='undefined'){AndroidBridge.reportHeight(document.body.scrollHeight)}},100)" +
        "})})" +
        "</script></body></html>"
}

private fun escapeHtmlPreservingMath(input: String): String {
    val result = StringBuilder()
    var i = 0
    val dollar = '\u0024'
    while (i < input.length) {
        when {
            i + 1 < input.length && input[i] == dollar && input[i + 1] == dollar -> {
                val marker = "\u0024\u0024"
                val end = input.indexOf(marker, i + 2)
                if (end >= 0) { result.append(input.substring(i, end + 2)); i = end + 2 }
                else { result.append(marker); i += 2 }
            }
            input[i] == dollar -> {
                val end = input.indexOf(dollar, i + 1)
                if (end >= 0) { result.append(input.substring(i, end + 1)); i = end + 1 }
                else { result.append(dollar); i += 1 }
            }
            input[i] == '\n' -> { result.append("<br>"); i += 1 }
            input[i] == '&' -> { result.append("&amp;"); i += 1 }
            input[i] == '<' -> { result.append("&lt;"); i += 1 }
            input[i] == '>' -> { result.append("&gt;"); i += 1 }
            else -> { result.append(input[i]); i += 1 }
        }
    }
    return result.toString()
}
