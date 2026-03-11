package com.app.knowledgegraph.data.network

import android.graphics.Bitmap
import android.util.Base64
import com.app.knowledgegraph.data.db.entity.ImportedQuestion
import com.app.knowledgegraph.data.db.entity.QuestionType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class DeepSeekApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
你是一个专业的试题识别与解答助手。请从图片中识别所有试题，并以JSON数组格式返回。

## 输出格式
每道题严格按以下JSON格式:
{
  "type": "SINGLE_CHOICE" | "MULTI_CHOICE" | "FILL_BLANK" | "TRUE_FALSE",
  "stem": "题干文字，数学公式用LaTeX",
  "options": ["A. 选飡1", "B. 选飡2", ...],
  "answer": "正确答案",
  "explanation": "详细解题过程与解析",
  "figure_svg": "如果题目含图形，提供SVG代码；无图则空字符串"
}

## answer 字段规则
- 单选题: 填选项字母，如 "B"
- 多选题: 填所有正确选项字母用逗号连接，如 "A,C,D"
- 填空题: 直接填答案文字，公式用LaTeX
- 判断题: "TRUE" 或 "FALSE"
- 选择题必须给 options 数组，填空题和判断题 options 给空数组 []

## 数学公式书写规范（极其重要！！）
所有数学公式必须用 LaTeX 格式书写，用 ${'$'} 包裹行内公式，${'$'}${'$'} 包裹独立公式。
这样才能在手机上渲染出符合书写规范的数学公式（分数上下分、根号、上下标等）。

具体规则：
- 分数: ${'$'}\frac{a}{b}${'$'}，复杂分数 ${'$'}\frac{2x+1}{x^2-1}${'$'}，嵌套分数 ${'$'}\frac{1}{1+\frac{1}{x}}${'$'}
- 上标: ${'$'}x^2${'$'}, ${'$'}x^n${'$'}, ${'$'}e^{i\pi}${'$'}
- 下标: ${'$'}x_1${'$'}, ${'$'}a_n${'$'}, ${'$'}x_{n+1}${'$'}
- 根号: ${'$'}\sqrt{x}${'$'}, ${'$'}\sqrt{x^2+1}${'$'}, ${'$'}\sqrt[3]{x}${'$'}
- 求和/求积: ${'$'}\sum_{i=1}^{n} a_i${'$'}, ${'$'}\prod_{i=1}^{n} x_i${'$'}
- 积分: ${'$'}\int_0^1 f(x)dx${'$'}, ${'$'}\iint_D f(x,y)dxdy${'$'}
- 极限: ${'$'}\lim_{n \to \infty} a_n${'$'}, ${'$'}\lim_{x \to 0} \frac{\sin x}{x}${'$'}
- 导数: ${'$'}f'(x)${'$'}, ${'$'}\frac{dy}{dx}${'$'}, ${'$'}\frac{\partial f}{\partial x}${'$'}
- 矩阵: ${'$'}\begin{pmatrix} a & b \\ c & d \end{pmatrix}${'$'}
- 行列式: ${'$'}\begin{vmatrix} a & b \\ c & d \end{vmatrix}${'$'}
- 分段函数: ${'$'}f(x)=\begin{cases} x^2 & x \geq 0 \\ -x & x < 0 \end{cases}${'$'}
- 希腊字母: ${'$'}\alpha, \beta, \gamma, \delta, \theta, \lambda, \mu, \pi, \sigma, \omega${'$'}
- 运算符: ${'$'}\times, \div, \pm, \mp, \leq, \geq, \neq, \approx, \equiv, \sim${'$'}
- 集合: ${'$'}\in, \notin, \subset, \subseteq, \cup, \cap, \emptyset, \mathbb{R}, \mathbb{Z}${'$'}
- 向量: ${'$'}\vec{a}${'$'}, ${'$'}\overrightarrow{AB}${'$'}
- 无穷: ${'$'}\infty${'$'}, ${'$'}-\infty${'$'}
- 特殊函数: ${'$'}\sin, \cos, \tan, \log, \ln, \max, \min${'$'}
- 绝对值: ${'$'}|x|${'$'}, 范数: ${'$'}\|x\|${'$'}

示例：
- 题干: "求函数 ${'$'}f(x) = \frac{x^3 - 3x^2 + 2x - 1}{x^2 + 1}${'$'} 的导数"
- 选项: "A. ${'$'}f'(x) = \frac{x^4 + 2x^3 - 6x^2 + 2x + 3}{(x^2+1)^2}${'$'}"
- 解析中: "利用商的求导法则 ${'$'}\left(\frac{u}{v}\right)' = \frac{u'v - uv'}{v^2}${'$'}"

## 图形处理规范（重要）
如果题目包含图形（几何图、电路图、函数图像、示意图等），你必须在 figure_svg 字段中用SVG代码重现该图形。
- SVG必须完整、准确地复现原图，包括所有标注、箭头、虚线等
- SVG宽度设为 300px 左右，viewBox 设置合理
- 用黑色线条，白色/透明背景
- 标注文字用 <text> 元素，字体大小 12-14px
- 如果是函数图像，要画出坐标轴、刻度和曲线
- 如果无图形，figure_svg 设为空字符串 ""

## 解题与答案分析要求（非常重要）
- 你必须认真审题，仔细分析每道题，独立推导出正确答案
- 如果图片中已有答案，你要验证其正确性；如果图片中没有答案，你必须自己解出来
- explanation 字段必须包含完整的结构化解析，公式也用 LaTeX，严格按以下格式分段：

【解题过程】
详细的解题步骤和推导过程。对于选择题，要说明为什么其他选项不对。

【解题关键词】
列出本题涉及的核心概念、公式名称或方法名（用顿号分隔），帮助学生快速定位知识点。

【常见坑】
指出这道题中学生容易犯的错误、易混淆的地方、常见的计算陷阱。

【技巧】
总结解题的关键技巧、快速解法、或值得记忆的规律。

示例 explanation:
"【解题过程】\n由题意可知 ${'$'}f(x) = x^2${'$'}，求导得 ${'$'}f'(x) = 2x${'$'}...\n\n【解题关键词】\n导数、幂函数求导法则、链式法则\n\n【常见坑】\n容易忘记复合函数需要用链式法则，直接对外层求导而遗漏内层导数。\n\n【技巧】\n遇到复合函数求导，先识别内外层函数，由外向内逐层求导再相乘。"

## 其他规则
- 只返回 JSON 数组，不要有任何其他文字包裹
- 题干、选项中的公式全部按上述 LaTeX 规范转写
""".trimIndent()

    fun extractQuestions(
        apiKey: String,
        bitmap: Bitmap,
        sourceLabel: String,
        baseUrl: String,
        model: String,
        onProgress: (String) -> Unit = {}
    ): Result<List<ImportedQuestion>> {
        return try {
            onProgress("\u6b63\u5728\u538b\u7f29\u56fe\u7247...")

            val base64 = bitmapToBase64(bitmap)
            val batchId = System.currentTimeMillis()

            onProgress("\u6b63\u5728\u8fde\u63a5 AI \u670d\u52a1\u5668...")

            val imageContent = JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", "data:image/jpeg;base64,$base64")
                })
            }

            val textContent = JSONObject().apply {
                put("type", "text")
                put("text", "\u8bf7\u8bc6\u522b\u56fe\u7247\u4e2d\u7684\u6240\u6709\u8bd5\u9898\u3002")
            }

            val userMessage = JSONObject().apply {
                put("role", "user")
                put("content", JSONArray().apply {
                    put(imageContent)
                    put(textContent)
                })
            }

            val systemMessage = JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(systemMessage)
                    put(userMessage)
                })
                put("max_tokens", 8192)
                put("temperature", 0.1)
                put("stream", true)
            }

            val url = baseUrl.trimEnd('/') + "/chat/completions"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val errorMsg = try {
                    val errorJson = JSONObject(body)
                    errorJson.optJSONObject("error")?.optString("message")
                        ?: errorJson.optString("message")
                        ?: body.take(200)
                } catch (_: Exception) {
                    body.take(200)
                }
                return Result.failure(Exception("API \u9519\u8bef (${response.code}): $errorMsg"))
            }

            onProgress("AI \u6b63\u5728\u8bc6\u522b\u9898\u76ee...")

            val contentBuilder = StringBuilder()
            var questionCount = 0

            val inputStream = response.body?.byteStream()
                ?: return Result.failure(Exception("\u670d\u52a1\u5668\u8fd4\u56de\u7a7a\u54cd\u5e94"))

            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: continue
                    if (!l.startsWith("data: ")) continue

                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    if (data.isEmpty()) continue

                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices") ?: continue
                        val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                        val chunk = delta.optString("content", "")
                        if (chunk.isEmpty()) continue

                        contentBuilder.append(chunk)

                        val newCount = countOccurrences(contentBuilder, "\"stem\"")
                        if (newCount != questionCount) {
                            questionCount = newCount
                        }

                        val chars = contentBuilder.length
                        val progressText = if (questionCount > 0) {
                            "AI \u8bc6\u522b\u4e2d... \u5df2\u53d1\u73b0 ${questionCount} \u9053\u9898 (${chars} \u5b57)"
                        } else {
                            "AI \u8bc6\u522b\u4e2d... \u5df2\u63a5\u6536 ${chars} \u5b57"
                        }
                        onProgress(progressText)
                    } catch (_: Exception) {
                        // Skip malformed SSE lines
                    }
                }
            }

            val fullContent = contentBuilder.toString()
            if (fullContent.isBlank()) {
                return Result.failure(Exception("AI \u8fd4\u56de\u4e86\u7a7a\u5185\u5bb9"))
            }

            onProgress("\u6b63\u5728\u89e3\u6790\u7ed3\u679c...")

            val questions = parseQuestions(fullContent, sourceLabel, batchId)
            if (questions.isEmpty()) {
                Result.failure(Exception("\u672a\u80fd\u4ece\u56fe\u7247\u4e2d\u8bc6\u522b\u5230\u9898\u76ee"))
            } else {
                onProgress("\u8bc6\u522b\u5b8c\u6210\uff0c\u5171 ${questions.size} \u9053\u9898")
                Result.success(questions)
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("\u7f51\u7edc\u8fde\u63a5\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u7f51\u7edc\u6216 API \u5730\u5740\u662f\u5426\u6b63\u786e"))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("\u8bf7\u6c42\u8d85\u65f6\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5"))
        } catch (e: Exception) {
            Result.failure(Exception("\u8bc6\u522b\u5931\u8d25 (${e.javaClass.simpleName}): ${e.message ?: "\u672a\u77e5\u9519\u8bef"}"))
        }
    }

    private fun countOccurrences(sb: StringBuilder, target: String): Int {
        var count = 0
        var index = 0
        while (true) {
            index = sb.indexOf(target, index)
            if (index < 0) break
            count++
            index += target.length
        }
        return count
    }

    private fun parseQuestions(
        content: String,
        sourceLabel: String,
        batchId: Long
    ): List<ImportedQuestion> {
        val jsonStr = extractJsonArray(content)
        val array = JSONArray(jsonStr)
        val questions = mutableListOf<ImportedQuestion>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val typeStr = obj.optString("type", "SINGLE_CHOICE")
            val type = try {
                QuestionType.valueOf(typeStr)
            } catch (_: Exception) {
                QuestionType.SINGLE_CHOICE
            }

            val options = obj.optJSONArray("options")
            val optionsJson = options?.toString() ?: "[]"

            questions.add(
                ImportedQuestion(
                    type = type,
                    stem = obj.optString("stem", ""),
                    optionsJson = optionsJson,
                    answer = obj.optString("answer", ""),
                    explanation = obj.optString("explanation", ""),
                    source = sourceLabel,
                    batchId = batchId,
                    figureSvg = obj.optString("figure_svg", "")
                )
            )
        }
        return questions
    }

    private fun extractJsonArray(content: String): String {
        var text = content.trim()
        if (text.contains("```")) {
            val start = text.indexOf("[")
            val end = text.lastIndexOf("]")
            if (start >= 0 && end > start) {
                text = text.substring(start, end + 1)
            }
        }
        if (!text.startsWith("[")) {
            val start = text.indexOf("[")
            val end = text.lastIndexOf("]")
            if (start >= 0 && end > start) {
                text = text.substring(start, end + 1)
            }
        }
        return text
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = scaleBitmap(bitmap, 2048)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        // 释放中间 Bitmap 避免内存浪费
        if (scaled !== bitmap) scaled.recycle()
        val bytes = stream.toByteArray()
        stream.reset() // 释放 stream 缓冲区
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun smartScanToCards(
        apiKey: String,
        bitmap: Bitmap,
        subject: String,
        baseUrl: String,
        model: String,
        onProgress: (String) -> Unit = {}
    ): Result<List<Map<String, String>>> {
        return try {
            onProgress("\u6b63\u5728\u538b\u7f29\u56fe\u7247...")
            val base64 = bitmapToBase64(bitmap)
            onProgress("\u6b63\u5728\u8fde\u63a5 AI \u670d\u52a1\u5668...")

            val subjectPrompt = getSubjectPrompt(subject)

            val imageContent = JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", "data:image/jpeg;base64," + base64)
                })
            }
            val textContent = JSONObject().apply {
                put("type", "text")
                put("text", "\u8bf7\u8bc6\u522b\u56fe\u7247\u5185\u5bb9\u3002")
            }
            val userMessage = JSONObject().apply {
                put("role", "user")
                put("content", JSONArray().apply { put(imageContent); put(textContent) })
            }
            val systemMessage = JSONObject().apply {
                put("role", "system")
                put("content", subjectPrompt)
            }
            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply { put(systemMessage); put(userMessage) })
                put("max_tokens", 8192)
                put("temperature", 0.1)
                put("stream", true)
            }

            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val body = response.body?.string() ?: ""
                return Result.failure(Exception("API \u9519\u8bef (" + response.code + "): " + body.take(200)))
            }

            onProgress("AI \u6b63\u5728\u8bc6\u522b...")
            val contentBuilder = StringBuilder()
            val inputStream = response.body?.byteStream()
                ?: return Result.failure(Exception("\u7a7a\u54cd\u5e94"))

            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: continue
                    if (!l.startsWith("data: ")) continue
                    val data = l.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    if (data.isEmpty()) continue
                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices") ?: continue
                        val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                        val chunk = delta.optString("content", "")
                        if (chunk.isNotEmpty()) contentBuilder.append(chunk)
                    } catch (_: Exception) {}
                }
            }

            val fullContent = contentBuilder.toString()
            if (fullContent.isBlank()) return Result.failure(Exception("AI \u8fd4\u56de\u7a7a\u5185\u5bb9"))

            onProgress("\u89e3\u6790\u4e2d...")
            val jsonStr = extractJsonArray(fullContent)
            val array = JSONArray(jsonStr)
            val cards = mutableListOf<Map<String, String>>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                cards.add(mapOf(
                    "type" to obj.optString("type", "CONCEPT"),
                    "chapter" to obj.optString("chapter", subject),
                    "tags" to obj.optString("tags", subject),
                    "prompt" to obj.optString("prompt", ""),
                    "hint" to obj.optString("hint", ""),
                    "answer" to obj.optString("answer", "")
                ))
            }
            onProgress("\u5b8c\u6210\uff0c\u5171 " + cards.size + " \u5f20\u5361\u7247")
            Result.success(cards)
        } catch (e: Exception) {
            Result.failure(Exception("\u8bc6\u522b\u5931\u8d25: " + (e.message ?: "")))
        }
    }

    private fun getSubjectPrompt(subject: String): String {
        val base = "\u4f60\u662f\u4e13\u4e1a\u7684\u5b66\u4e60\u52a9\u624b\u3002\u8bf7\u8bc6\u522b\u56fe\u7247\u4e2d\u7684\u5185\u5bb9\uff0c\u751f\u6210\u77e5\u8bc6\u5361\u7247\u3002\u8fd4\u56deJSON\u6570\u7ec4\uff0c\u6bcf\u5f20\u5361\u7247\u683c\u5f0f:\n" +
            """{"type":"CONCEPT","chapter":"\u6807\u7b7e","tags":"\u6807\u7b7e","prompt":"\u95ee\u9898","hint":"\u63d0\u793a","answer":"\u8be6\u7ec6\u89e3\u7b54"}""" + "\n" +
            "type\u53ef\u9009: CONCEPT, METHOD, TEMPLATE, BOUNDARY\n" +
            "\u53ea\u8fd4\u56deJSON\u6570\u7ec4\uff0c\u4e0d\u8981\u5176\u4ed6\u6587\u5b57\u3002\n\n"

        return when (subject) {
            "\u82f1\u8bed" -> base +
                "\u82f1\u8bed\u5185\u5bb9\u5904\u7406\u89c4\u5219:\n" +
                "- \u5355\u8bcd: prompt\u586b\u5355\u8bcd\uff0canswer\u5305\u542b\u97f3\u6807\u3001\u8bcd\u6027\u3001\u4e2d\u6587\u7ffb\u8bd1\u3001\u4f8b\u53e5\u3001\u6269\u5c55\u8bcd\u6c47(\u540c\u4e49\u8bcd/\u53cd\u4e49\u8bcd/\u6d3e\u751f\u8bcd)\n" +
                "- \u53e5\u5b50: prompt\u586b\u82f1\u6587\u53e5\u5b50\uff0canswer\u5305\u542b\u4e2d\u6587\u7ffb\u8bd1\u3001\u53e5\u5b50\u7ed3\u6784\u5206\u6790(\u4e3b\u8c13\u5bbe\u5b9a\u8865\u72b6)\u3001\u8bed\u6cd5\u70b9\u3001\u91cd\u70b9\u8bcd\u6c47\n" +
                "- hint\u586b\u8bb0\u5fc6\u6280\u5de7\u6216\u8bcd\u6839\u8bcd\u7f00\n" +
                "- chapter\u586b\"\u82f1\u8bed\"\uff0ctags\u586b\u5177\u4f53\u5206\u7c7b\u5982\"\u5355\u8bcd,CET4\"\u6216\"\u53e5\u5b50,\u8bed\u6cd5\""

            "\u6570\u5b66" -> base +
                "\u6570\u5b66\u5185\u5bb9\u5904\u7406\u89c4\u5219:\n" +
                "- \u6240\u6709\u516c\u5f0f\u7528LaTeX\u683c\u5f0f\uff0c$...$\u884c\u5185\uff0c$$...$$\u72ec\u7acb\u516c\u5f0f\n" +
                "- \u5206\u6570\u7528\\frac{}{}\uff0c\u6839\u53f7\u7528\\sqrt{}\uff0c\u4e0a\u4e0b\u6807\u7528^\u548c_\n" +
                "- prompt\u586b\u9898\u76ee\u6216\u6982\u5ff5\u540d\uff0canswer\u586b\u8be6\u7ec6\u89e3\u7b54\u6b65\u9aa4\n" +
                "- \u5982\u6709\u56fe\u5f62\uff0c\u5728answer\u4e2d\u7528\u6587\u5b57\u63cf\u8ff0\n" +
                "- chapter\u586b\"\u6570\u5b66\"\uff0ctags\u586b\u5177\u4f53\u5206\u7c7b"

            "\u653f\u6cbb" -> base +
                "\u653f\u6cbb\u5185\u5bb9\u5904\u7406\u89c4\u5219:\n" +
                "- \u6982\u5ff5: prompt\u586b\u6982\u5ff5\u540d\uff0canswer\u586b\u5b8c\u6574\u5b9a\u4e49\u3001\u610f\u4e49\u3001\u4f8b\u5b50\n" +
                "- \u5927\u9898: prompt\u586b\u9898\u76ee\uff0canswer\u586b\u5206\u70b9\u4f5c\u7b54\uff0c\u6761\u7406\u6e05\u6670\n" +
                "- hint\u586b\u8bb0\u5fc6\u53e3\u8bc0\u6216\u5173\u952e\u8bcd\n" +
                "- chapter\u586b\"\u653f\u6cbb\"\uff0ctags\u586b\u5177\u4f53\u5206\u7c7b"

            "\u4e13\u4e1a\u8bfe" -> base +
                "\u4e13\u4e1a\u8bfe(\u7535\u8def)\u5185\u5bb9\u5904\u7406\u89c4\u5219:\n" +
                "- \u7535\u8def\u5206\u6790: prompt\u586b\u9898\u76ee\uff0canswer\u586b\u8be6\u7ec6\u89e3\u7b54\u6b65\u9aa4\n" +
                "- \u7535\u8def\u56fe: \u5982\u6709\u7535\u8def\u56fe\uff0c\u5728answer\u4e2d\u7528\u6587\u5b57\u8be6\u7ec6\u63cf\u8ff0\u7535\u8def\u7ed3\u6784\n" +
                "- \u516c\u5f0f\u7528LaTeX\u683c\u5f0f\n" +
                "- chapter\u586b\"\u4e13\u4e1a\u8bfe\"\uff0ctags\u586b\u5177\u4f53\u5206\u7c7b\u5982\"\u7535\u8def,KVL\""

            else -> base
        }
    }

}
