package com.app.knowledgegraph.ui.components

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.knowledgegraph.ui.theme.Primary
import java.util.Locale

/**
 * 英语单词发音按钮
 * - 自动提取文本中的英文部分
 * - 只有检测到英文才显示
 * - 使用 Android TTS 引擎朗读
 */
@Composable
fun TtsButton(
    text: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 20.dp,
    locale: Locale = Locale.US
) {
    val englishText = remember(text) { extractEnglish(text) }
    if (englishText.isBlank()) return

    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isReady by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    // 设置语言（每次 locale 变化时）
    LaunchedEffect(isReady, locale) {
        if (isReady) {
            tts?.language = locale
        }
    }

    IconButton(
        onClick = {
            if (isReady) {
                tts?.speak(englishText, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
            }
        },
        modifier = modifier.size(iconSize + 12.dp),
        colors = IconButtonDefaults.iconButtonColors(contentColor = Primary)
    ) {
        Icon(Icons.Default.VolumeUp, contentDescription = "发音", modifier = Modifier.size(iconSize))
    }
}

/**
 * 判断文本是否包含需要发音的英文内容
 * 用于外部判断是否显示 TTS 按钮
 */
fun hasEnglishContent(text: String): Boolean {
    return extractEnglish(text).isNotBlank()
}

/**
 * 判断卡片是否应该显示发音按钮
 * - chapter 包含"英语" → 显示
 * - prompt 中有实质英文单词 → 显示
 */
fun shouldShowTts(chapter: String, prompt: String): Boolean {
    if (chapter.contains("英语", ignoreCase = true)) return true
    if (chapter.contains("english", ignoreCase = true)) return true
    // 检查 prompt 中是否有 3 个以上英文单词
    val words = extractEnglish(prompt).split("\\s+".toRegex()).filter { it.length >= 2 }
    return words.size >= 2
}

/**
 * 从混合文本中提取英文部分
 * - 去掉 LaTeX 公式（$...$）
 * - 去掉中文字符
 * - 去掉标点，只保留英文单词
 */
private fun extractEnglish(text: String): String {
    // 先去掉 LaTeX
    val noLatex = text
        .replace(Regex("\\$\\$[^$]+\\$\\$"), " ")
        .replace(Regex("\\$[^$]+\\$"), " ")
        .replace(Regex("\\\\[a-zA-Z]+\\{[^}]*}"), " ")

    // 提取英文单词（至少2个字母）
    val words = Regex("[a-zA-Z][a-zA-Z'\\-]{1,}").findAll(noLatex)
        .map { it.value }
        .filter { word ->
            // 过滤掉常见的 LaTeX/代码关键词
            word.lowercase() !in EXCLUDE_WORDS
        }
        .toList()

    return words.joinToString(" ")
}

private val EXCLUDE_WORDS = setOf(
    "dp", "px", "sp", "id", "val", "var", "fun", "int", "null",
    "true", "false", "if", "else", "for", "when", "is", "in",
    "frac", "sqrt", "sum", "sin", "cos", "tan", "log", "ln",
    "alpha", "beta", "gamma", "delta", "theta", "lambda", "pi",
    "infty", "cdot", "times", "div", "pm", "mp", "le", "ge",
    "eq", "ne", "lt", "gt", "mod", "max", "min", "lim"
)
