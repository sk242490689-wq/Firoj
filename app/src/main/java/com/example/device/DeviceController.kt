package com.example.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.SystemTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder

class DeviceController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private var cameraIdWithFlash: String? = null

    init {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraIdWithFlash = id
                    return@forEach
                }
            }
            if (cameraIdWithFlash == null && (cameraManager?.cameraIdList?.isNotEmpty() == true)) {
                cameraIdWithFlash = cameraManager.cameraIdList[0]
            }

            cameraManager?.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    super.onTorchModeChanged(cameraId, enabled)
                    if (cameraId == cameraIdWithFlash) {
                        _isTorchOn.value = enabled
                    }
                }
            }, null)
        } catch (_: Exception) {}
    }

    fun toggleFlashlight(forceState: Boolean? = null): Boolean {
        val target = cameraIdWithFlash ?: return false
        val newState = forceState ?: !_isTorchOn.value
        return try {
            cameraManager?.setTorchMode(target, newState)
            _isTorchOn.value = newState
            true
        } catch (e: Exception) {
            false
        }
    }

    fun launchApp(packageName: String, appName: String, fallbackUrl: String? = null): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                // Fallback action if app is not directly installed
                when {
                    packageName.contains("instagram") -> {
                        openUrl("https://www.instagram.com")
                    }
                    packageName.contains("youtube") -> {
                        openUrl("https://www.youtube.com")
                    }
                    packageName.contains("whatsapp") -> {
                        openUrl("https://web.whatsapp.com")
                    }
                    packageName.contains("camera") -> {
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(cameraIntent)
                        true
                    }
                    fallbackUrl != null -> {
                        openUrl(fallbackUrl)
                    }
                    else -> {
                        // Open Play Store details
                        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(marketIntent)
                        true
                    }
                }
            }
        } catch (e: Exception) {
            fallbackUrl?.let { openUrl(it) } ?: false
        }
    }

    fun openDialer(phoneNumber: String = ""): Boolean {
        return try {
            val uri = if (phoneNumber.isNotBlank()) {
                val clean = phoneNumber.filter { it.isDigit() || it == '+' }
                Uri.parse("tel:$clean")
            } else {
                Uri.parse("tel:")
            }
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openSettings(target: String): Boolean {
        return try {
            val action = when (target.lowercase()) {
                "wifi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
                "display" -> Settings.ACTION_DISPLAY_SETTINGS
                "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun openUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun searchWeb(query: String): Boolean {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.google.com/search?q=$encoded"
            openUrl(url)
        } catch (e: Exception) {
            false
        }
    }

    fun searchYouTube(query: String): Boolean {
        return try {
            val pm = context.packageManager
            val ytIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encoded"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (ytIntent != null) {
                    setPackage("com.google.android.youtube")
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            openUrl("https://www.youtube.com/results?search_query=${URLEncoder.encode(query, "UTF-8")}")
        }
    }

    fun getLiveTelemetry(): SystemTelemetry {
        var batteryPct = 100
        var isCharging = false
        var batteryTempC = 29.0f

        try {
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, batteryFilter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    batteryPct = (level * 100) / scale
                }
                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val rawTemp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 290)
                batteryTempC = rawTemp / 10.0f
            }
        } catch (_: Exception) {}

        // RAM info
        var availRam = 2048L
        var totalRam = 4096L
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            availRam = memInfo.availMem / (1024 * 1024)
            totalRam = memInfo.totalMem / (1024 * 1024)
        } catch (_: Exception) {}

        // Storage info
        var freeStorage = 16L
        var totalStorage = 64L
        try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val blockSize = statFs.blockSizeLong
            freeStorage = (statFs.availableBlocksLong * blockSize) / (1024 * 1024 * 1024)
            totalStorage = (statFs.blockCountLong * blockSize) / (1024 * 1024 * 1024)
        } catch (_: Exception) {}

        // Network
        var isOnline = false
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val capabilities = cm?.getNetworkCapabilities(activeNetwork)
            isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (_: Exception) {}

        return SystemTelemetry(
            batteryPct = batteryPct,
            isCharging = isCharging,
            batteryTemperatureC = batteryTempC,
            availableRamMb = availRam,
            totalRamMb = totalRam,
            freeStorageGb = freeStorage,
            totalStorageGb = totalStorage,
            isFlashlightOn = _isTorchOn.value,
            networkConnected = isOnline,
            corePowerLevel = ((batteryPct * 0.7f) + (if (isOnline) 30 else 10)).toInt().coerceIn(15, 100)
        )
    }
}
