package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CommandEntity
import com.example.data.JarvisAction
import com.example.data.JarvisAiService
import com.example.data.JarvisDatabase
import com.example.data.JarvisStatus
import com.example.data.SystemTelemetry
import com.example.device.DeviceController
import com.example.device.JarvisSpeechManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class HudTab {
    CORE,
    TRANSCRIPT,
    APPS,
    DIAGNOSTICS
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    private val commandDao = db.commandDao()
    private val deviceController = DeviceController(application)
    private val aiService = JarvisAiService(application)

    val commandHistory: StateFlow<List<CommandEntity>> = commandDao.getAllCommands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _status = MutableStateFlow(JarvisStatus.IDLE)
    val status: StateFlow<JarvisStatus> = _status.asStateFlow()

    private val _telemetry = MutableStateFlow(deviceController.getLiveTelemetry())
    val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

    private val _currentResponse = MutableStateFlow<String?>(null)
    val currentResponse: StateFlow<String?> = _currentResponse.asStateFlow()

    private val _activeTab = MutableStateFlow(HudTab.CORE)
    val activeTab: StateFlow<HudTab> = _activeTab.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    val isTorchOn: StateFlow<Boolean> = deviceController.isTorchOn

    // Speech manager
    val speechManager = JarvisSpeechManager(
        context = application,
        onVoiceResult = { voiceText ->
            processUserCommand(voiceText, isVoice = true)
        },
        onError = { errorMsg ->
            _status.value = JarvisStatus.IDLE
            _toastMessage.value = errorMsg
        }
    )

    val isListening: StateFlow<Boolean> = speechManager.isListening
    val isSpeaking: StateFlow<Boolean> = speechManager.isSpeaking
    val audioAmplitude: StateFlow<Float> = speechManager.audioAmplitude
    val isMuted: StateFlow<Boolean> = speechManager.speechMuted

    private var telemetryJob: Job? = null

    init {
        startTelemetryLoop()
        // Welcome message on startup
        viewModelScope.launch {
            delay(800)
            val welcomeText = "J.A.R.V.I.S. Core online. All Stark systems calibrated and standing by, Sir."
            _currentResponse.value = welcomeText
            speechManager.speak(welcomeText)
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob = viewModelScope.launch {
            while (isActive) {
                _telemetry.value = deviceController.getLiveTelemetry()
                delay(3000)
            }
        }
    }

    fun setTab(tab: HudTab) {
        _activeTab.value = tab
    }

    fun showApiKeyDialog(show: Boolean) {
        _showApiKeyDialog.value = show
    }

    fun saveCustomApiKey(key: String) {
        aiService.setCustomApiKey(key)
        _toastMessage.value = "Gemini API key updated, Sir."
    }

    fun hasApiKey(): Boolean = aiService.hasApiKey()
    fun getApiKey(): String = aiService.getApiKey()

    fun toggleSpeechMute() {
        speechManager.toggleSpeechMute()
    }

    fun startVoiceInput() {
        if (_status.value == JarvisStatus.LISTENING) {
            speechManager.stopListening()
            _status.value = JarvisStatus.IDLE
        } else {
            _status.value = JarvisStatus.LISTENING
            speechManager.startListening()
        }
    }

    fun processUserCommand(prompt: String, isVoice: Boolean = false) {
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _status.value = JarvisStatus.THINKING
            val currentStats = _telemetry.value

            val parsed = aiService.queryJarvis(prompt, currentStats)

            _currentResponse.value = parsed.speechResponse
            _status.value = JarvisStatus.EXECUTING

            // Execute the action if any
            var actionPayload: String? = null
            when (val act = parsed.action) {
                is JarvisAction.LaunchApp -> {
                    actionPayload = act.packageName
                    deviceController.launchApp(act.packageName, act.appName, act.fallbackUrl)
                }
                is JarvisAction.CallPhone -> {
                    actionPayload = act.numberOrName
                    deviceController.openDialer(act.numberOrName)
                }
                is JarvisAction.ToggleTorch -> {
                    deviceController.toggleFlashlight(act.turnOn)
                    actionPayload = "Torch: ${act.turnOn ?: "toggle"}"
                }
                is JarvisAction.OpenSettings -> {
                    actionPayload = act.target
                    deviceController.openSettings(act.target)
                }
                is JarvisAction.PlayYouTube -> {
                    actionPayload = act.query
                    deviceController.searchYouTube(act.query)
                }
                is JarvisAction.WebSearch -> {
                    actionPayload = act.query
                    deviceController.searchWeb(act.query)
                }
                is JarvisAction.FetchWeather -> {
                    actionPayload = act.city
                }
                is JarvisAction.RunProtocol -> {
                    actionPayload = act.protocolName
                    executeProtocol(act.protocolName)
                }
                JarvisAction.None -> {}
            }

            // Speak out response
            _status.value = JarvisStatus.SPEAKING
            speechManager.speak(parsed.speechResponse)

            // Save to Room DB
            commandDao.insertCommand(
                CommandEntity(
                    query = prompt,
                    response = parsed.speechResponse,
                    commandType = parsed.commandType,
                    isVoice = isVoice,
                    actionPayload = actionPayload
                )
            )

            delay(2000)
            if (_status.value == JarvisStatus.EXECUTING || _status.value == JarvisStatus.SPEAKING) {
                _status.value = JarvisStatus.IDLE
            }
        }
    }

    private fun executeProtocol(protocolName: String) {
        when (protocolName) {
            "House Party" -> {
                viewModelScope.launch {
                    repeat(4) {
                        deviceController.toggleFlashlight(true)
                        delay(250)
                        deviceController.toggleFlashlight(false)
                        delay(250)
                    }
                    deviceController.searchYouTube("ACDC Back In Black Stark")
                }
            }
            "Clean Slate" -> {
                viewModelScope.launch {
                    commandDao.clearHistory()
                    _toastMessage.value = "Memory cache cleared. Core purged."
                }
            }
        }
    }

    fun toggleTorchDirect() {
        val success = deviceController.toggleFlashlight()
        if (success) {
            val state = deviceController.isTorchOn.value
            val text = if (state) "Auxiliary photon beam activated." else "Torch deactivated, Sir."
            speechManager.speak(text)
        }
    }

    fun launchAppDirect(packageName: String, appName: String, fallbackUrl: String? = null) {
        val success = deviceController.launchApp(packageName, appName, fallbackUrl)
        val text = if (success) "Launching $appName, Sir." else "Unable to launch $appName."
        speechManager.speak(text)
    }

    fun openDialerDirect(number: String = "") {
        deviceController.openDialer(number)
        speechManager.speak("Communications dialer ready, Sir.")
    }

    fun openSettingsDirect(target: String) {
        deviceController.openSettings(target)
        speechManager.speak("Displaying $target controls, Sir.")
    }

    fun clearHistory() {
        viewModelScope.launch {
            commandDao.clearHistory()
            _toastMessage.value = "Command records purged, Sir."
            speechManager.speak("Transcript memory cleared, Sir.")
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun repeatSpeech(text: String) {
        speechManager.speak(text)
    }

    override fun onCleared() {
        super.onCleared()
        telemetryJob?.cancel()
        speechManager.shutdown()
    }
}
