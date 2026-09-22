package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FrameStats
import com.example.model.SpotPixelData
import com.example.omt.ConnectionStatus
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.MedicalAmber
import com.example.ui.theme.MedicalCyan
import com.example.ui.theme.MedicalEmerald
import com.example.ui.theme.MedicalRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun OmtViewport(
    bitmap: Bitmap?,
    connectionStatus: ConnectionStatus,
    statusMessage: String,
    frameStats: FrameStats,
    isFrozen: Boolean,
    showHud: Boolean,
    zoomScale: Float,
    panOffset: Offset,
    crosshairNorm: Offset?,
    spotPixelData: SpotPixelData?,
    onTap: (normX: Float, normY: Float) -> Unit,
    onDoubleTap: () -> Unit,
    onTransform: (zoomDelta: Float, panDelta: Offset) -> Unit,
    onClearCrosshair: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onTransform(zoom, pan)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onTap = { offset ->
                        val normX = (offset.x / size.width).coerceIn(0f, 1f)
                        val normY = (offset.y / size.height).coerceIn(0f, 1f)
                        onTap(normX, normY)
                    }
                )
            }
            .testTag("omt_viewport_canvas")
    ) {
        val containerWidth = maxWidth.value
        val containerHeight = maxHeight.value

        // Bitmap Rendering
        if (bitmap != null) {
            val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                // Aspect ratio fit
                val bmpW = bitmap.width.toFloat()
                val bmpH = bitmap.height.toFloat()
                val scale = minOf(canvasW / bmpW, canvasH / bmpH) * zoomScale

                val destW = bmpW * scale
                val destH = bmpH * scale
                val destX = (canvasW - destW) / 2f + panOffset.x
                val destY = (canvasH - destH) / 2f + panOffset.y

                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(destX.roundToInt(), destY.roundToInt()),
                    dstSize = IntSize(destW.roundToInt(), destH.roundToInt())
                )

                // Draw precision crosshair on frame if placed
                crosshairNorm?.let { norm ->
                    val crossX = destX + norm.x * destW
                    val crossY = destY + norm.y * destH

                    if (crossX in 0f..canvasW && crossY in 0f..canvasH) {
                        // Outer reticle ring
                        drawCircle(
                            color = MedicalCyan,
                            radius = 18f,
                            center = Offset(crossX, crossY),
                            style = Stroke(width = 2f)
                        )
                        // Inner dot
                        drawCircle(
                            color = MedicalEmerald,
                            radius = 3f,
                            center = Offset(crossX, crossY)
                        )
                        // Horizontal cross lines
                        drawLine(
                            color = MedicalCyan,
                            start = Offset(crossX - 30f, crossY),
                            end = Offset(crossX + 30f, crossY),
                            strokeWidth = 1.5f
                        )
                        // Vertical cross lines
                        drawLine(
                            color = MedicalCyan,
                            start = Offset(crossX, crossY - 30f),
                            end = Offset(crossX, crossY + 30f),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }
        } else {
            // Empty / Waiting State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    when (connectionStatus) {
                        ConnectionStatus.CONNECTING -> {
                            CircularProgressIndicator(
                                color = MedicalCyan,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            Text(
                                text = "Negotiating OMT TCP Connection...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MedicalCyan)
                            )
                        }
                        ConnectionStatus.ERROR -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MedicalRed,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Text(
                                text = "Stream Connection Error",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MedicalRed,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                        else -> {
                            Text(
                                text = "OMT Standby",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Select an active stream from the chooser above or connect via IP:Port",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Overlay: Telemetry HUD
        if (showHud && connectionStatus == ConnectionStatus.CONNECTED) {
            Surface(
                color = DarkCanvas.copy(alpha = 0.82f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .testTag("telemetry_hud_panel")
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MedicalEmerald, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OMT LOW-LATENCY FEED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MedicalCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.size(4.dp))

                    Text(
                        text = "RES: ${frameStats.resolution} | FMT: ${frameStats.format}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "FPS: %.1f | BITRATE: %.2f Mbps".format(frameStats.fps, frameStats.bitrateMbps),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MedicalEmerald,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "LATENCY: %.1f ms | FRAMES: %d".format(frameStats.latencyMs, frameStats.totalFrames),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    )

                    if (zoomScale > 1f) {
                        Text(
                            text = "ZOOM: %.1fx (Double-tap to reset)".format(zoomScale),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MedicalAmber,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        // Overlay: Freeze Badge
        if (isFrozen) {
            Surface(
                color = MedicalAmber.copy(alpha = 0.9f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = null,
                        tint = DarkCanvas,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FROZEN FRAME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DarkCanvas,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Overlay: Spot Pixel Inspection Pill
        spotPixelData?.let { spot ->
            Surface(
                color = DarkSurfaceElevated.copy(alpha = 0.92f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MedicalCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .testTag("spot_inspector_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    // Color swatch
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(spot.red, spot.green, spot.blue), RoundedCornerShape(3.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "COORD: (%d, %d)".format(spot.x, spot.y),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MedicalCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "LUM: %d | RGB: (%d, %d, %d)".format(spot.luminance, spot.red, spot.green, spot.blue),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "EST. DENSITY: %d HU".format(spot.estimatedHU),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MedicalEmerald,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onClearCrosshair,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Crosshair",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
