package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.CommandInputBar
import com.example.ui.components.CoreDashboardView
import com.example.ui.components.DiagnosticsView
import com.example.ui.components.HudHeader
import com.example.ui.components.QuickCommandsGrid
import com.example.ui.components.TranscriptView
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.HudTab
import com.example.viewmodel.JarvisViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                JarvisApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun JarvisApp(viewModel: JarvisViewModel) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val currentResponse by viewModel.currentResponse.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val commandHistory by viewModel.commandHistory.collectAsStateWithLifecycle()
    val audioAmplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Handle back button: return to CORE if in another tab
    BackHandler(enabled = activeTab != HudTab.CORE) {
        viewModel.setTab(HudTab.CORE)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavyBg),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HudHeader(
                telemetry = telemetry,
                hasApiKey = viewModel.hasApiKey(),
                isMuted = isMuted,
                activeTab = activeTab,
                onTabSelected = { viewModel.setTab(it) },
                onToggleMute = { viewModel.toggleSpeechMute() },
                onOpenApiKeyDialog = { viewModel.showApiKeyDialog(true) }
            )
        },
        bottomBar = {
            CommandInputBar(
                isListening = isListening,
                onStartVoice = { viewModel.startVoiceInput() },
                onSubmitText = { text -> viewModel.processUserCommand(text, isVoice = false) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepNavyBg)
                .padding(innerPadding)
        ) {
            when (activeTab) {
                HudTab.CORE -> {
                    CoreDashboardView(
                        status = status,
                        audioAmplitude = audioAmplitude,
                        isSpeaking = isSpeaking,
                        isListening = isListening,
                        currentResponse = currentResponse,
                        onCoreClick = { viewModel.startVoiceInput() },
                        onQuickPrompt = { prompt -> viewModel.processUserCommand(prompt, isVoice = false) },
                        onRepeatSpeech = { text -> viewModel.repeatSpeech(text) }
                    )
                }

                HudTab.TRANSCRIPT -> {
                    TranscriptView(
                        commands = commandHistory,
                        onReplaySpeech = { text -> viewModel.repeatSpeech(text) },
                        onClearHistory = { viewModel.clearHistory() },
                        onSuggestionClick = { prompt ->
                            viewModel.processUserCommand(prompt, isVoice = false)
                            viewModel.setTab(HudTab.CORE)
                        }
                    )
                }

                HudTab.APPS -> {
                    QuickCommandsGrid(
                        onCommandClick = { prompt -> viewModel.processUserCommand(prompt, isVoice = false) },
                        onLaunchInstagram = { viewModel.launchAppDirect("com.instagram.android", "Instagram", "https://instagram.com") },
                        onLaunchYouTube = { viewModel.launchAppDirect("com.google.android.youtube", "YouTube", "https://youtube.com") },
                        onLaunchWhatsApp = { viewModel.launchAppDirect("com.whatsapp", "WhatsApp", "https://web.whatsapp.com") },
                        onLaunchCamera = { viewModel.launchAppDirect("com.android.camera", "Camera") },
                        onOpenDialer = { viewModel.openDialerDirect() },
                        onToggleTorch = { viewModel.toggleTorchDirect() },
                        isTorchActive = isTorchOn,
                        onOpenSettings = { target -> viewModel.openSettingsDirect(target) },
                        onRunHouseParty = { viewModel.processUserCommand("Protocol House Party", isVoice = false) }
                    )
                }

                HudTab.DIAGNOSTICS -> {
                    DiagnosticsView(
                        telemetry = telemetry,
                        onRunScan = { viewModel.processUserCommand("Diagnostics report", isVoice = false) }
                    )
                }
            }
        }
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            currentKey = viewModel.getApiKey(),
            hasKey = viewModel.hasApiKey(),
            onSaveKey = { newKey -> viewModel.saveCustomApiKey(newKey) },
            onDismiss = { viewModel.showApiKeyDialog(false) }
        )
    }
}
