package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SensorsOff
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FrameStats
import com.example.model.OmtSourceType
import com.example.model.OmtStreamSource
import com.example.omt.ConnectionStatus
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.MedicalAmber
import com.example.ui.theme.MedicalCyan
import com.example.ui.theme.MedicalEmerald
import com.example.ui.theme.MedicalRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OmtTopBar(
    selectedStream: OmtStreamSource?,
    discoveredStreams: List<OmtStreamSource>,
    connectionStatus: ConnectionStatus,
    frameStats: FrameStats,
    isScanning: Boolean,
    isFrozen: Boolean,
    showHud: Boolean,
    onSelectStream: (OmtStreamSource) -> Unit,
    onRefreshScan: () -> Unit,
    onOpenManualDialog: () -> Unit,
    onToggleFreeze: () -> Unit,
    onToggleHud: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Surface(
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // First Row: App Branding + Status + Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing status dot
                    val statusColor = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> MedicalEmerald
                        ConnectionStatus.CONNECTING -> MedicalAmber
                        ConnectionStatus.ERROR -> MedicalRed
                        else -> TextMuted
                    }

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Df-OMTViewer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MedicalCyan,
                            fontSize = 17.sp,
                            letterSpacing = 0.5.sp
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "OMT v1.0",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier
                            .background(DarkSurfaceElevated, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                // Action icon buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // HUD toggle
                    IconButton(
                        onClick = onToggleHud,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("toggle_hud_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Toggle HUD Overlay",
                            tint = if (showHud) MedicalCyan else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Freeze Frame Button
                    IconButton(
                        onClick = onToggleFreeze,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("freeze_frame_button")
                    ) {
                        Icon(
                            imageVector = if (isFrozen) Icons.Default.PlayArrow else Icons.Outlined.Pause,
                            contentDescription = if (isFrozen) "Resume Live Stream" else "Freeze Frame",
                            tint = if (isFrozen) MedicalAmber else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Manual Connect IP Button
                    IconButton(
                        onClick = onOpenManualDialog,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("manual_ip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLink,
                            contentDescription = "Manual IP Connect",
                            tint = MedicalCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Scan / Refresh Button
                    IconButton(
                        onClick = onRefreshScan,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("refresh_discovery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Discover OMT Streams",
                            tint = if (isScanning) MedicalCyan else TextSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isScanning) rotationAngle else 0f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Second Row: Stream Chooser Dropdown Bar
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("stream_chooser_dropdown"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (connectionStatus == ConnectionStatus.CONNECTED) Icons.Default.Sensors else Icons.Default.SensorsOff,
                            contentDescription = null,
                            tint = if (connectionStatus == ConnectionStatus.CONNECTED) MedicalEmerald else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedStream?.name ?: "Select an OMT Stream",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = selectedStream?.displayAddress ?: "No stream selected",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (connectionStatus == ConnectionStatus.CONNECTED && frameStats.fps > 0) {
                            Text(
                                text = "%.1f FPS".format(frameStats.fps),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MedicalEmerald,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .background(DarkSurface, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Expand Streams",
                            tint = MedicalCyan
                        )
                    }
                }

                // Dropdown Menu List
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "AVAILABLE OMT STREAMS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MedicalCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )

                    HorizontalDivider(color = DarkSurfaceBorder)

                    if (discoveredStreams.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No streams found on LAN", color = TextMuted) },
                            onClick = { }
                        )
                    } else {
                        discoveredStreams.forEach { stream ->
                            val isSelected = stream.id == selectedStream?.id
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stream.name,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) MedicalCyan else TextPrimary
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (stream.sourceType == OmtSourceType.LOCAL_LOOPBACK) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "LOCAL CAM",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MedicalCyan,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        modifier = Modifier
                                                            .background(DarkSurface, RoundedCornerShape(3.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                } else if (stream.sourceType == OmtSourceType.LAN_DISCOVERED) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "LAN",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = MedicalEmerald,
                                                            fontSize = 9.sp
                                                        ),
                                                        modifier = Modifier
                                                            .background(DarkSurface, RoundedCornerShape(3.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${stream.displayAddress} | ${stream.resolution}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(MedicalCyan, CircleShape)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectStream(stream)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = DarkSurfaceBorder)

                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AddLink,
                                    contentDescription = null,
                                    tint = MedicalCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connect via Custom IP / Port...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MedicalCyan,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        },
                        onClick = {
                            dropdownExpanded = false
                            onOpenManualDialog()
                        }
                    )
                }
            }
        }
    }
}
