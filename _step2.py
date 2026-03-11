path = r"app\src\main\java\com\app\knowledgegraph\data\network\DeepSeekApi.kt"
with open(path, "r", encoding="utf-8") as f:
    c = f.read()

# Add smartScan method before the last closing brace of the class
new_method = '''
    fun smartScanToCards(
        apiKey: String,
        bitmap: Bitmap,
        subject: String,
        baseUrl: String,
        model: String,
        onProgress: (String) -> Unit = {}
    ): Result<List<Map<String, String>>> {
        return try {
            onProgress("\\u6b63\\u5728\\u538b\\u7f29\\u56fe\\u7247...")
            val base64 = bitmapToBase64(bitmap)
            onProgress("\\u6b63\\u5728\\u8fde\\u63a5 AI \\u670d\\u52a1\\u5668...")

            val subjectPrompt = getSubjectPrompt(subject)

            val imageContent = JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", "data:image/jpeg;base64," + base64)
                })
            }
            val textContent = JSONObject().apply {
                put("type", "text")
                put("text", "\\u8bf7\\u8bc6\\u522b\\u56fe\\u7247\\u5185\\u5bb9\\u3002")
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
                return Result.failure(Exception("API \\u9519\\u8bef (" + response.code + "): " + body.take(200)))
            }

            onProgress("AI \\u6b63\\u5728\\u8bc6\\u522b...")
            val contentBuilder = StringBuilder()
            val inputStream = response.body?.byteStream()
                ?: return Result.failure(Exception("\\u7a7a\\u54cd\\u5e94"))

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
            if (fullContent.isBlank()) return Result.failure(Exception("AI \\u8fd4\\u56de\\u7a7a\\u5185\\u5bb9"))

            onProgress("\\u89e3\\u6790\\u4e2d...")
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
            onProgress("\\u5b8c\\u6210\\uff0c\\u5171 " + cards.size + " \\u5f20\\u5361\\u7247")
            Result.success(cards)
        } catch (e: Exception) {
            Result.failure(Exception("\\u8bc6\\u522b\\u5931\\u8d25: " + (e.message ?: "")))
        }
    }

    private fun getSubjectPrompt(subject: String): String {
        val base = "\\u4f60\\u662f\\u4e13\\u4e1a\\u7684\\u5b66\\u4e60\\u52a9\\u624b\\u3002\\u8bf7\\u8bc6\\u522b\\u56fe\\u7247\\u4e2d\\u7684\\u5185\\u5bb9\\uff0c\\u751f\\u6210\\u77e5\\u8bc6\\u5361\\u7247\\u3002\\u8fd4\\u56deJSON\\u6570\\u7ec4\\uff0c\\u6bcf\\u5f20\\u5361\\u7247\\u683c\\u5f0f:\\n" +
            """{"type":"CONCEPT","chapter":"\\u6807\\u7b7e","tags":"\\u6807\\u7b7e","prompt":"\\u95ee\\u9898","hint":"\\u63d0\\u793a","answer":"\\u8be6\\u7ec6\\u89e3\\u7b54"}""" + "\\n" +
            "type\\u53ef\\u9009: CONCEPT, METHOD, TEMPLATE, BOUNDARY\\n" +
            "\\u53ea\\u8fd4\\u56deJSON\\u6570\\u7ec4\\uff0c\\u4e0d\\u8981\\u5176\\u4ed6\\u6587\\u5b57\\u3002\\n\\n"

        return when (subject) {
            "\\u82f1\\u8bed" -> base +
                "\\u82f1\\u8bed\\u5185\\u5bb9\\u5904\\u7406\\u89c4\\u5219:\\n" +
                "- \\u5355\\u8bcd: prompt\\u586b\\u5355\\u8bcd\\uff0canswer\\u5305\\u542b\\u97f3\\u6807\\u3001\\u8bcd\\u6027\\u3001\\u4e2d\\u6587\\u7ffb\\u8bd1\\u3001\\u4f8b\\u53e5\\u3001\\u6269\\u5c55\\u8bcd\\u6c47(\\u540c\\u4e49\\u8bcd/\\u53cd\\u4e49\\u8bcd/\\u6d3e\\u751f\\u8bcd)\\n" +
                "- \\u53e5\\u5b50: prompt\\u586b\\u82f1\\u6587\\u53e5\\u5b50\\uff0canswer\\u5305\\u542b\\u4e2d\\u6587\\u7ffb\\u8bd1\\u3001\\u53e5\\u5b50\\u7ed3\\u6784\\u5206\\u6790(\\u4e3b\\u8c13\\u5bbe\\u5b9a\\u8865\\u72b6)\\u3001\\u8bed\\u6cd5\\u70b9\\u3001\\u91cd\\u70b9\\u8bcd\\u6c47\\n" +
                "- hint\\u586b\\u8bb0\\u5fc6\\u6280\\u5de7\\u6216\\u8bcd\\u6839\\u8bcd\\u7f00\\n" +
                "- chapter\\u586b\\"\\u82f1\\u8bed\\"\\uff0ctags\\u586b\\u5177\\u4f53\\u5206\\u7c7b\\u5982\\"\\u5355\\u8bcd,CET4\\"\\u6216\\"\\u53e5\\u5b50,\\u8bed\\u6cd5\\""

            "\\u6570\\u5b66" -> base +
                "\\u6570\\u5b66\\u5185\\u5bb9\\u5904\\u7406\\u89c4\\u5219:\\n" +
                "- \\u6240\\u6709\\u516c\\u5f0f\\u7528LaTeX\\u683c\\u5f0f\\uff0c$...$\\u884c\\u5185\\uff0c$$...$$\\u72ec\\u7acb\\u516c\\u5f0f\\n" +
                "- \\u5206\\u6570\\u7528\\\\frac{}{}\\uff0c\\u6839\\u53f7\\u7528\\\\sqrt{}\\uff0c\\u4e0a\\u4e0b\\u6807\\u7528^\\u548c_\\n" +
                "- prompt\\u586b\\u9898\\u76ee\\u6216\\u6982\\u5ff5\\u540d\\uff0canswer\\u586b\\u8be6\\u7ec6\\u89e3\\u7b54\\u6b65\\u9aa4\\n" +
                "- \\u5982\\u6709\\u56fe\\u5f62\\uff0c\\u5728answer\\u4e2d\\u7528\\u6587\\u5b57\\u63cf\\u8ff0\\n" +
                "- chapter\\u586b\\"\\u6570\\u5b66\\"\\uff0ctags\\u586b\\u5177\\u4f53\\u5206\\u7c7b"

            "\\u653f\\u6cbb" -> base +
                "\\u653f\\u6cbb\\u5185\\u5bb9\\u5904\\u7406\\u89c4\\u5219:\\n" +
                "- \\u6982\\u5ff5: prompt\\u586b\\u6982\\u5ff5\\u540d\\uff0canswer\\u586b\\u5b8c\\u6574\\u5b9a\\u4e49\\u3001\\u610f\\u4e49\\u3001\\u4f8b\\u5b50\\n" +
                "- \\u5927\\u9898: prompt\\u586b\\u9898\\u76ee\\uff0canswer\\u586b\\u5206\\u70b9\\u4f5c\\u7b54\\uff0c\\u6761\\u7406\\u6e05\\u6670\\n" +
                "- hint\\u586b\\u8bb0\\u5fc6\\u53e3\\u8bc0\\u6216\\u5173\\u952e\\u8bcd\\n" +
                "- chapter\\u586b\\"\\u653f\\u6cbb\\"\\uff0ctags\\u586b\\u5177\\u4f53\\u5206\\u7c7b"

            "\\u4e13\\u4e1a\\u8bfe" -> base +
                "\\u4e13\\u4e1a\\u8bfe(\\u7535\\u8def)\\u5185\\u5bb9\\u5904\\u7406\\u89c4\\u5219:\\n" +
                "- \\u7535\\u8def\\u5206\\u6790: prompt\\u586b\\u9898\\u76ee\\uff0canswer\\u586b\\u8be6\\u7ec6\\u89e3\\u7b54\\u6b65\\u9aa4\\n" +
                "- \\u7535\\u8def\\u56fe: \\u5982\\u6709\\u7535\\u8def\\u56fe\\uff0c\\u5728answer\\u4e2d\\u7528\\u6587\\u5b57\\u8be6\\u7ec6\\u63cf\\u8ff0\\u7535\\u8def\\u7ed3\\u6784\\n" +
                "- \\u516c\\u5f0f\\u7528LaTeX\\u683c\\u5f0f\\n" +
                "- chapter\\u586b\\"\\u4e13\\u4e1a\\u8bfe\\"\\uff0ctags\\u586b\\u5177\\u4f53\\u5206\\u7c7b\\u5982\\"\\u7535\\u8def,KVL\\""

            else -> base
        }
    }
'''

# Insert before the last } of the class
last_brace = c.rfind("}")
c = c[:last_brace] + new_method + "\n" + c[last_brace:]

# Fix unicode escapes - they should stay as \\uXXXX in Kotlin source
# Actually in Kotlin, \\u inside regular strings creates unicode chars, which is what we want

with open(path, "w", encoding="utf-8") as f:
    f.write(c)
print("done: DeepSeekApi smartScan")
