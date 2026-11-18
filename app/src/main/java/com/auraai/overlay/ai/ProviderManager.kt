package com.auraai.overlay.ai

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object ProviderManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences("auraai_prefs", Context.MODE_PRIVATE)

    // ── ENTRY POINT ──────────────────────────────────────────────
    fun ask(prefs: SharedPreferences, provider: AiProvider, prompt: String): String {
        return when (provider) {
            AiProvider.AUTO -> askCascade(prefs, prompt)
            AiProvider.GEMINI -> askGemini(prefs, prompt)
            AiProvider.GROQ -> askGroq(prefs, prompt)
            AiProvider.MISTRAL -> askMistral(prefs, prompt)
            AiProvider.COHERE -> askCohere(prefs, prompt)
            AiProvider.TOGETHER -> askTogether(prefs, prompt)
            AiProvider.OPENAI -> askOpenAI(prefs, prompt)
            AiProvider.POLLINATIONS -> askPollinations(prompt)
        }
    }

    // ── CASCADE: coba satu-satu sampai berhasil ──────────────────
    private fun askCascade(prefs: SharedPreferences, prompt: String): String {
        val order = listOf(
            { askPollinations(prompt) },              // No key needed — first try
            { askGemini(prefs, prompt) },
            { askGroq(prefs, prompt) },
            { askMistral(prefs, prompt) },
            { askCohere(prefs, prompt) },
            { askTogether(prefs, prompt) },
            { askOpenAI(prefs, prompt) },
        )
        for (fn in order) {
            try {
                val result = fn()
                if (result.isNotBlank() && !result.startsWith("ERROR")) return result
            } catch (_: Exception) {}
        }
        return "Tidak dapat terhubung ke AI. Cek koneksi internet."
    }

    // ── POLLINATIONS (gratis, tanpa API key) ─────────────────────
    private fun askPollinations(prompt: String): String {
        val encoded = prompt.take(500).replace(" ", "+")
        val request = Request.Builder()
            .url("https://text.pollinations.ai/$encoded")
            .get()
            .build()
        val response = client.newCall(request).execute()
        return response.body?.string()?.trim() ?: "ERROR"
    }

    // ── GEMINI ───────────────────────────────────────────────────
    private fun askGemini(prefs: SharedPreferences, prompt: String): String {
        val key = prefs.getString("key_gemini", "") ?: ""
        if (key.isBlank()) return "ERROR: No Gemini key"

        val body = """
        {
          "contents": [{"parts": [{"text": "${ prompt.escape() }"}]}],
          "generationConfig": {"maxOutputTokens": 1024, "temperature": 0.1}
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=$key")
            .post(body.toRequestBody(JSON))
            .build()

        val response = client.newCall(request).execute()
        val json = JsonParser.parseString(response.body?.string()).asJsonObject
        return json.getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString
            ?: "ERROR: Gemini parse failed"
    }

    // ── GROQ (Llama 3 — fast & free tier) ───────────────────────
    private fun askGroq(prefs: SharedPreferences, prompt: String): String {
        val key = prefs.getString("key_groq", "") ?: ""
        if (key.isBlank()) return "ERROR: No Groq key"
        return askOpenAICompat(
            "https://api.groq.com/openai/v1/chat/completions",
            key,
            "llama3-8b-8192",
            prompt
        )
    }

    // ── MISTRAL ──────────────────────────────────────────────────
    private fun askMistral(prefs: SharedPreferences, prompt: String): String {
        val key = prefs.getString("key_mistral", "") ?: ""
        if (key.isBlank()) return "ERROR: No Mistral key"
        return askOpenAICompat(
            "https://api.mistral.ai/v1/chat/completions",
            key,
            "mistral-small-latest",
            prompt
        )
    }

    // ── TOGETHER AI ──────────────────────────────────────────────
    private fun askTogether(prefs: SharedPreferences, prompt: String): String {
        val key = prefs.getString("key_together", "") ?: ""
        if (key.isBlank()) return "ERROR: No Together key"
        return askOpenAICompat(
            "https://api.together.xyz/v1/chat/completions",
            key,
            "meta-llama/Llama-3.2-11B-Vision-Instruct-Turbo",
            prompt
        )
    }

    // ── OPENAI ───────────────────────────────────────────────────
    private fun askOpenAI(prefs: SharedPreferences, prompt: String): String {
        val key = prefs.getString("key_openai", "") ?: ""
        if (key.isBlank()) return "ERROR: No OpenAI key"
        return askOpenAICompat(
            "https://api.openai.com/v1/chat/completions",
            key,
            "gpt-4o-mini",
            prompt
        )
    }

    // ── COHERE ───────────────────────────────────────────────────
    private fun askCohere(prefs: SharedPreferences, prompt: String): String {
        val key = prefs.getString("key_cohere", "") ?: ""
        if (key.isBlank()) return "ERROR: No Cohere key"

        val body = """
        {
          "model": "command-r",
          "message": "${ prompt.escape() }",
          "max_tokens": 1024,
          "temperature": 0.1
        }
        """.trimIndent()

        val request = Request.Builder()
            .url("https://api.cohere.ai/v1/chat")
            .post(body.toRequestBody(JSON))
            .header("Authorization", "Bearer $key")
            .build()

        val response = client.newCall(request).execute()
        val json = JsonParser.parseString(response.body?.string()).asJsonObject
        return json.get("text")?.asString ?: "ERROR: Cohere parse failed"
    }

    // ── SHARED OpenAI-compat format ───────────────────────────────
    private fun askOpenAICompat(url: String, key: String, model: String, prompt: String): String {
        val sysPrompt = "Kamu adalah asisten ujian cerdas. Jawab soal dengan akurat dan ringkas. Jika pilihan ganda, sebutkan jawaban yang benar. Jika essay, berikan jawaban lengkap beserta langkah."

        val body = """
        {
          "model": "$model",
          "messages": [
            {"role": "system", "content": "${ sysPrompt.escape() }"},
            {"role": "user", "content": "${ prompt.escape() }"}
          ],
          "max_tokens": 1024,
          "temperature": 0.1
        }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody(JSON))
            .header("Authorization", "Bearer $key")
            .build()

        val response = client.newCall(request).execute()
        val json = JsonParser.parseString(response.body?.string()).asJsonObject
        return json.getAsJsonArray("choices")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("message")
            ?.get("content")?.asString
            ?: "ERROR: parse failed"
    }

    private fun String.escape() = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
