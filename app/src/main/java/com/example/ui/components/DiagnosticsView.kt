package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SystemTelemetry
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HudBorderCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.StarkGold
import com.example.ui.theme.StarkRedAlert
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DiagnosticsView(
    telemetry: SystemTelemetry,
    onRunScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Diagnostic Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "// TELEMETRY READOUTS",
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "REAL-TIME HARDWARE SENSOR PROBE",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Button(
                onClick = onRunScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceElevated,
                    contentColor = CyberCyan
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .border(1.dp, HudBorderCyan, RoundedCornerShape(6.dp))
                    .testTag("scan_systems_button")
            ) {
                Text(
                    text = "CALIBRATE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ARC REACTOR POWER & THERMAL METRIC
        MetricCard(
            title = "ARC POWER MATRIX",
            icon = Icons.Default.BatteryChargingFull,
            value = "${telemetry.batteryPct}%",
            subValue = if (telemetry.isCharging) "AUXILIARY CHARGING ACTIVE" else "AUTONOMOUS DISCHARGE",
            progress = telemetry.batteryPct / 100f,
            progressColor = if (telemetry.batteryPct < 20) StarkRedAlert else CyberCyan
        )

        // CORE TEMPERATURE
        MetricCard(
            title = "CORE THERMAL SENSORS",
            icon = Icons.Default.Thermostat,
            value = "${telemetry.batteryTemperatureC} °C",
            subValue = if (telemetry.batteryTemperatureC > 42f) "CRITICAL ELEVATION" else "OPTIMAL RUNNING TEMP",
            progress = (telemetry.batteryTemperatureC / 60f).coerceIn(0f, 1f),
            progressColor = if (telemetry.batteryTemperatureC > 40f) StarkRedAlert else StarkGold
        )

        // RAM USAGE
        val usedRam = (telemetry.totalRamMb - telemetry.availableRamMb).coerceAtLeast(0)
        val ramFraction = if (telemetry.totalRamMb > 0) usedRam.toFloat() / telemetry.totalRamMb else 0.5f
        MetricCard(
            title = "VOLATILE SYSTEM RAM",
            icon = Icons.Default.Memory,
            value = "$usedRam MB / ${telemetry.totalRamMb} MB",
            subValue = "${telemetry.availableRamMb} MB AVAILABLE HEAP",
            progress = ramFraction,
            progressColor = ElectricBlue
        )

        // STORAGE USAGE
        val usedStorage = (telemetry.totalStorageGb - telemetry.freeStorageGb).coerceAtLeast(0)
        val storageFraction = if (telemetry.totalStorageGb > 0) usedStorage.toFloat() / telemetry.totalStorageGb else 0.5f
        MetricCard(
            title = "INTERNAL DATA VAULT",
            icon = Icons.Default.Storage,
            value = "$usedStorage GB / ${telemetry.totalStorageGb} GB",
            subValue = "${telemetry.freeStorageGb} GB FREE SECTOR SPACE",
            progress = storageFraction,
            progressColor = CyberCyan
        )

        // AUXILIARY SUBSYSTEM STATUS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceCard)
                .border(1.dp, HudBorderCyan, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "// HARDWARE ARRAY STATUS",
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            StatusRow(name = "NEURAL LINK / NETWORK", status = if (telemetry.networkConnected) "CONNECTED" else "OFFLINE", isGood = telemetry.networkConnected)
            StatusRow(name = "AUX PHOTON TORCH", status = if (telemetry.isFlashlightOn) "EMITTING" else "STANDBY", isGood = telemetry.isFlashlightOn)
            StatusRow(name = "STARK SECURITY ENCLAVE", status = "ARMED", isGood = true)
            StatusRow(name = "VOICE PARSER PROTOCOL", status = "ONLINE", isGood = true)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
private fun MetricCard(
    title: String,
    icon: ImageVector,
    value: String,
    subValue: String,
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, HudBorderCyan, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = progressColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = DarkSurfaceElevated
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subValue,
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StatusRow(
    name: String,
    status: String,
    isGood: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = status,
            color = if (isGood) NeonGreen else StarkGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
