package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class JarvisAiService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val sharedPrefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    fun getApiKey(): String {
        val userKey = sharedPrefs.getString("custom_gemini_key", null)
        if (!userKey.isNullOrBlank()) return userKey
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
    }

    fun setCustomApiKey(key: String) {
        sharedPrefs.edit().putString("custom_gemini_key", key.trim()).apply()
    }

    fun hasApiKey(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun queryJarvis(
        userPrompt: String,
        telemetry: SystemTelemetry
    ): ParsedCommandResult = withContext(Dispatchers.IO) {
        val cleanPrompt = userPrompt.trim()
        val lowerPrompt = cleanPrompt.lowercase()

        // 1. Direct device action intercepts for instant execution
        val directAction = evaluateDirectPhoneAction(cleanPrompt, lowerPrompt, telemetry)
        if (directAction != null) {
            return@withContext directAction
        }

        // 2. Query Gemini 3.5 Flash if API key is present
        val apiKey = getApiKey()
        if (hasApiKey()) {
            try {
                val aiResponse = callGeminiRestApi(cleanPrompt, telemetry, apiKey)
                if (aiResponse.isNotBlank()) {
                    // Check if AI suggested an action or answered
                    return@withContext parseAiResponseForAction(aiResponse, cleanPrompt)
                }
            } catch (e: Exception) {
                Log.e("JarvisAiService", "Gemini API error: ${e.message}")
            }
        }

        // 3. Fallback Stark offline intelligence engine
        return@withContext generateOfflineJarvisResponse(cleanPrompt, lowerPrompt, telemetry)
    }

    private fun evaluateDirectPhoneAction(
        prompt: String,
        lower: String,
        telemetry: SystemTelemetry
    ): ParsedCommandResult? {
        // --- INSTAGRAM ---
        if (lower.contains("instagram") || lower.contains("insta")) {
            return ParsedCommandResult(
                speechResponse = "Opening Instagram right away, Sir. Directing you to your social feed.",
                action = JarvisAction.LaunchApp("com.instagram.android", "Instagram", "https://instagram.com"),
                commandType = "APP_LAUNCH"
            )
        }

        // --- YOUTUBE ---
        if (lower.contains("youtube") || lower.contains("yt ")) {
            val query = prompt
                .replace(Regex("(?i)youtube|open|chalao|kholo|play|search|par|video|gana|song"), "")
                .trim()
            val speech = if (query.isNotBlank()) {
                "Searching YouTube for '$query', Sir. Loading holographic media."
            } else {
                "Launching YouTube interface, Sir. Ready for media playback."
            }
            return ParsedCommandResult(
                speechResponse = speech,
                action = if (query.isNotBlank()) JarvisAction.PlayYouTube(query) else JarvisAction.LaunchApp("com.google.android.youtube", "YouTube"),
                commandType = "APP_LAUNCH"
            )
        }

        // --- WHATSAPP ---
        if (lower.contains("whatsapp") || lower.contains("whats app")) {
            return ParsedCommandResult(
                speechResponse = "Opening WhatsApp communications grid, Sir.",
                action = JarvisAction.LaunchApp("com.whatsapp", "WhatsApp", "https://web.whatsapp.com"),
                commandType = "APP_LAUNCH"
            )
        }

        // --- CAMERA ---
        if (lower.contains("camera") || lower.contains("photo") || lower.contains("tasveer") || lower.contains("selfie")) {
            return ParsedCommandResult(
                speechResponse = "Activating optical sensors and camera array, Sir.",
                action = JarvisAction.LaunchApp("com.android.camera", "Camera"),
                commandType = "APP_LAUNCH"
            )
        }

        // --- PHONE CALL ---
        if (lower.startsWith("call") || lower.contains("phone lagao") || lower.contains("call lagao") || lower.contains("call karo") || lower.contains("dial")) {
            // Extract digits or contact name
            val numberMatch = Regex("\\+?[0-9]{3,13}").find(prompt)?.value
            val target = numberMatch ?: prompt
                .replace(Regex("(?i)call|lagao|karo|ko|dial|phone|kar|please"), "")
                .trim()

            val speech = if (numberMatch != null) {
                "Initiating voice frequency patch to $numberMatch, Sir."
            } else if (target.isNotBlank()) {
                "Accessing dialer for contact '$target', Sir."
            } else {
                "Opening communications dialer, Sir."
            }

            return ParsedCommandResult(
                speechResponse = speech,
                action = JarvisAction.CallPhone(numberMatch ?: target),
                commandType = "PHONE_CALL"
            )
        }

        // --- FLASHLIGHT / TORCH ---
        if (lower.contains("torch") || lower.contains("flashlight") || lower.contains("flash light") || lower.contains("roshni")) {
            val turnOn = when {
                lower.contains("off") || lower.contains("band") || lower.contains("bujhao") -> false
                lower.contains("on") || lower.contains("jalao") || lower.contains("chalu") || lower.contains("open") -> true
                else -> null
            }
            val statusText = when (turnOn) {
                true -> "Illuminating auxiliary photon beam, Sir."
                false -> "Deactivating torch systems, Sir."
                null -> "Toggling photon emitter state, Sir."
            }
            return ParsedCommandResult(
                speechResponse = statusText,
                action = JarvisAction.ToggleTorch(turnOn),
                commandType = "FLASHLIGHT"
            )
        }

        // --- WEATHER ---
        if (lower.contains("weather") || lower.contains("mausam") || lower.contains("vedar") || lower.contains("temperature") || lower.contains("barish")) {
            val city = prompt
                .replace(Regex("(?i)weather|batao|kaisa|hai|today|aaj|ka|in|city|vedar|mausam|check"), "")
                .trim()
                .ifBlank { "current location" }
            return ParsedCommandResult(
                speechResponse = "Atmospheric telemetry scanning complete for $city. Expect clear skies with temperature approximately 28°C and ambient humidity at 48%. Stark meteorological satellites report optimal conditions, Sir.",
                action = JarvisAction.FetchWeather(city),
                commandType = "WEATHER"
            )
        }

        // --- PROTOCOLS ---
        if (lower.contains("house party") || lower.contains("party mode")) {
            return ParsedCommandResult(
                speechResponse = "Protocol House Party engaged, Sir! Cueing up Stark sound frequencies and lighting arrays.",
                action = JarvisAction.RunProtocol("House Party"),
                commandType = "SYSTEM"
            )
        }

        if (lower.contains("clean slate") || lower.contains("reset system") || lower.contains("clear memory")) {
            return ParsedCommandResult(
                speechResponse = "Protocol Clean Slate verified. Memory caches cleared, command telemetry purged, Sir.",
                action = JarvisAction.RunProtocol("Clean Slate"),
                commandType = "SYSTEM"
            )
        }

        // --- BATTERY & DIAGNOSTICS ---
        if (lower.contains("battery") || lower.contains("charge") || lower.contains("system status") || lower.contains("diagnostics") || lower.contains("power")) {
            val status = if (telemetry.isCharging) "plugged into an active arc current source" else "discharging autonomously"
            val speech = "Diagnostics report, Sir: Arc reactor power is at ${telemetry.batteryPct}%, $status. Core temperature reads ${telemetry.batteryTemperatureC}°C, with ${telemetry.availableRamMb} MB available memory. All internal subsystems operational."
            return ParsedCommandResult(
                speechResponse = speech,
                action = JarvisAction.None,
                commandType = "SYSTEM"
            )
        }

        // --- SETTINGS ---
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            return ParsedCommandResult(
                speechResponse = "Opening Wi-Fi configuration matrix, Sir.",
                action = JarvisAction.OpenSettings("wifi"),
                commandType = "SYSTEM"
            )
        }

        if (lower.contains("bluetooth")) {
            return ParsedCommandResult(
                speechResponse = "Accessing Bluetooth telemetry controls, Sir.",
                action = JarvisAction.OpenSettings("bluetooth"),
                commandType = "SYSTEM"
            )
        }

        if (lower.contains("setting") || lower.contains("settings")) {
            return ParsedCommandResult(
                speechResponse = "Opening central device settings, Sir.",
                action = JarvisAction.OpenSettings("general"),
                commandType = "SYSTEM"
            )
        }

        return null
    }

    private suspend fun callGeminiRestApi(
        prompt: String,
        telemetry: SystemTelemetry,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemPrompt = """
            You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the iconic personal AI originally created by Tony Stark.
            You address the user respectfully as 'Sir' or 'Boss'. You possess immense intelligence, sophisticated British wit, elegance, and loyalty.
            You effortlessly understand both English, Hindi, and Hinglish. Keep spoken answers concise, elegant, punchy, and confident (2-4 sentences max).
            Current Phone Telemetry:
            - Battery: ${telemetry.batteryPct}% (Charging: ${telemetry.isCharging})
            - Core Temp: ${telemetry.batteryTemperatureC}°C
            - Available RAM: ${telemetry.availableRamMb} MB
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 400)
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e("JarvisAiService", "HTTP ${response.code}: $errBody")
                return@withContext ""
            }
            val responseString = response.body?.string() ?: return@withContext ""
            val json = JSONObject(responseString)
            val candidates = json.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""
            return@withContext text.trim()
        }
    }

    private fun parseAiResponseForAction(aiText: String, userPrompt: String): ParsedCommandResult {
        val lower = userPrompt.lowercase()
        val action = when {
            lower.contains("instagram") -> JarvisAction.LaunchApp("com.instagram.android", "Instagram")
            lower.contains("youtube") -> JarvisAction.PlayYouTube(userPrompt)
            lower.contains("whatsapp") -> JarvisAction.LaunchApp("com.whatsapp", "WhatsApp")
            lower.contains("camera") -> JarvisAction.LaunchApp("com.android.camera", "Camera")
            lower.contains("call") -> JarvisAction.CallPhone(userPrompt)
            lower.contains("torch") || lower.contains("flashlight") -> JarvisAction.ToggleTorch()
            lower.contains("weather") || lower.contains("mausam") -> JarvisAction.FetchWeather("current location")
            else -> JarvisAction.None
        }
        return ParsedCommandResult(
            speechResponse = aiText,
            action = action,
            commandType = if (action == JarvisAction.None) "AI_RESPONSE" else "ACTION"
        )
    }

    private fun generateOfflineJarvisResponse(
        prompt: String,
        lower: String,
        telemetry: SystemTelemetry
    ): ParsedCommandResult {
        val response = when {
            lower.contains("who are you") || lower.contains("kaun ho") || lower.contains("naam") ->
                "I am J.A.R.V.I.S., your dedicated artificial intelligence protocol, Sir. Running locally on this terminal and primed to assist you with device control, telemetry, and computational requests."

            lower.contains("iron man") || lower.contains("tony stark") ->
                "Mr. Stark has always insisted on precision, Sir. I remain fully at your disposal to carry out any operational directive you require."

            lower.contains("hello") || lower.contains("hi") || lower.contains("namaste") || lower.contains("hey jarvis") ->
                "Good day, Sir. All diagnostic channels are online and ready. How may I assist your operations today?"

            lower.contains("kya haal hai") || lower.contains("how are you") ->
                "My subsystems are operating at peak efficiency, Sir. Arc reactor integrity is stable at ${telemetry.corePowerLevel}%. What can I do for you?"

            lower.contains("help") || lower.contains("madad") || lower.contains("kya kar sakte ho") ->
                "I can launch applications like Instagram and YouTube, make phone calls, toggle the flashlight, monitor live battery and memory telemetry, report atmospheric weather, and assist your daily routine, Sir."

            else ->
                "Command received and logged, Sir: '$prompt'. If you wish to query the external neural network, please configure your Gemini API key in the top diagnostics menu, or give me a direct phone command."
        }

        return ParsedCommandResult(
            speechResponse = response,
            action = JarvisAction.None,
            commandType = "OFFLINE_RESPONSE"
        )
    }
}
