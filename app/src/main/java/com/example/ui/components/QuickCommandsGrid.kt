package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HudBorderBright
import com.example.ui.theme.HudBorderCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.StarkGold
import com.example.ui.theme.StarkRedAlert
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class QuickActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun QuickCommandsGrid(
    onCommandClick: (String) -> Unit,
    onLaunchInstagram: () -> Unit,
    onLaunchYouTube: () -> Unit,
    onLaunchWhatsApp: () -> Unit,
    onLaunchCamera: () -> Unit,
    onOpenDialer: () -> Unit,
    onToggleTorch: () -> Unit,
    isTorchActive: Boolean,
    onOpenSettings: (String) -> Unit,
    onRunHouseParty: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        QuickActionItem(
            title = "INSTAGRAM",
            subtitle = "Open Feed / Stories",
            icon = Icons.Default.Share,
            accentColor = Color(0xFFE1306C),
            testTag = "action_instagram",
            onClick = onLaunchInstagram
        ),
        QuickActionItem(
            title = "YOUTUBE",
            subtitle = "Launch Media Grid",
            icon = Icons.Default.PlayArrow,
            accentColor = Color(0xFFFF0000),
            testTag = "action_youtube",
            onClick = onLaunchYouTube
        ),
        QuickActionItem(
            title = "WHATSAPP",
            subtitle = "Messaging Channel",
            icon = Icons.Default.Share,
            accentColor = Color(0xFF25D366),
            testTag = "action_whatsapp",
            onClick = onLaunchWhatsApp
        ),
        QuickActionItem(
            title = "PHONE CALL",
            subtitle = "Dialer & Voice Comms",
            icon = Icons.Default.Call,
            accentColor = NeonGreen,
            testTag = "action_call",
            onClick = onOpenDialer
        ),
        QuickActionItem(
            title = if (isTorchActive) "TORCH [ON]" else "FLASHLIGHT",
            subtitle = if (isTorchActive) "Aux Photon Active" else "Toggle Torch",
            icon = Icons.Default.FlashlightOn,
            accentColor = if (isTorchActive) StarkGold else CyberCyan,
            testTag = "action_torch",
            onClick = onToggleTorch
        ),
        QuickActionItem(
            title = "CAMERA",
            subtitle = "Optical Sensors",
            icon = Icons.Default.CameraAlt,
            accentColor = ElectricBlue,
            testTag = "action_camera",
            onClick = onLaunchCamera
        ),
        QuickActionItem(
            title = "ATMOSPHERIC",
            subtitle = "Live Weather Scan",
            icon = Icons.Default.Cloud,
            accentColor = CyberCyan,
            testTag = "action_weather",
            onClick = { onCommandClick("What's the weather today?") }
        ),
        QuickActionItem(
            title = "HOUSE PARTY",
            subtitle = "Stark Celebration",
            icon = Icons.Default.Celebration,
            accentColor = StarkGold,
            testTag = "action_house_party",
            onClick = onRunHouseParty
        ),
        QuickActionItem(
            title = "SYSTEM SETTINGS",
            subtitle = "Hardware Controls",
            icon = Icons.Default.Settings,
            accentColor = TextSecondary,
            testTag = "action_settings",
            onClick = { onOpenSettings("general") }
        ),
        QuickActionItem(
            title = "PURGE CACHE",
            subtitle = "Protocol Clean Slate",
            icon = Icons.Default.Refresh,
            accentColor = StarkRedAlert,
            testTag = "action_clean_slate",
            onClick = { onCommandClick("Protocol Clean Slate") }
        )
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "// RAPID ACCESS PROTOCOLS",
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "SYSTEM ACTIVE",
                color = NeonGreen,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(actions) { item ->
                QuickActionCard(item = item)
            }
        }
    }
}

@Composable
fun QuickActionCard(
    item: QuickActionItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, HudBorderCyan, RoundedCornerShape(10.dp))
            .clickable(onClick = item.onClick)
            .testTag(item.testTag)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(item.accentColor.copy(alpha = 0.15f))
                    .border(1.dp, item.accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}
