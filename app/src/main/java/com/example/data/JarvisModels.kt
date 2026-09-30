package com.example.data

enum class JarvisStatus {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING
}

data class SystemTelemetry(
    val batteryPct: Int = 100,
    val isCharging: Boolean = false,
    val batteryTemperatureC: Float = 28.5f,
    val availableRamMb: Long = 2048,
    val totalRamMb: Long = 4096,
    val freeStorageGb: Long = 32,
    val totalStorageGb: Long = 128,
    val isFlashlightOn: Boolean = false,
    val networkConnected: Boolean = true,
    val corePowerLevel: Int = 98
)

sealed class JarvisAction {
    data class LaunchApp(val packageName: String, val appName: String, val fallbackUrl: String? = null) : JarvisAction()
    data class CallPhone(val numberOrName: String) : JarvisAction()
    data class ToggleTorch(val turnOn: Boolean? = null) : JarvisAction()
    data class FetchWeather(val city: String) : JarvisAction()
    data class OpenSettings(val target: String) : JarvisAction()
    data class WebSearch(val query: String) : JarvisAction()
    data class PlayYouTube(val query: String) : JarvisAction()
    data class RunProtocol(val protocolName: String) : JarvisAction()
    object None : JarvisAction()
}

data class ParsedCommandResult(
    val speechResponse: String,
    val action: JarvisAction,
    val commandType: String
)
