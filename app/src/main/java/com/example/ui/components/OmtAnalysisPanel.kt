package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ColorMapMode
import com.example.model.FrameStats
import com.example.model.OmtStreamSource
import com.example.model.WindowLevelPreset
import com.example.omt.ConnectionStatus
import com.example.ui.AnalysisTab
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.MedicalAmber
import com.example.ui.theme.MedicalCyan
import com.example.ui.theme.MedicalEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OmtAnalysisPanel(
    activeTab: AnalysisTab,
    onTabSelected: (AnalysisTab) -> Unit,
    windowWidth: Float,
    windowCenter: Float,
    onWindowLevelChanged: (width: Float, center: Float) -> Unit,
    presets: List<WindowLevelPreset>,
    onApplyPreset: (WindowLevelPreset) -> Unit,
    colorMapMode: ColorMapMode,
    onColorMapChanged: (ColorMapMode) -> Unit,
    histogramData: IntArray,
    frameStats: FrameStats,
    selectedStream: OmtStreamSource?,
    connectionStatus: ConnectionStatus
) {
    Surface(
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tab Row
            TabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = DarkSurface,
                contentColor = MedicalCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                        color = MedicalCyan,
                        height = 3.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                AnalysisTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MedicalCyan else TextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Tab Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                when (activeTab) {
                    AnalysisTab.WINDOW_LEVEL -> {
                        WindowLevelControls(
                            windowWidth = windowWidth,
                            windowCenter = windowCenter,
                            onWindowLevelChanged = onWindowLevelChanged,
                            presets = presets,
                            onApplyPreset = onApplyPreset
                        )
                    }
                    AnalysisTab.COLOR_MAP -> {
                        ColorMapControls(
                            currentMode = colorMapMode,
                            onModeSelected = onColorMapChanged
                        )
                    }
                    AnalysisTab.HISTOGRAM -> {
                        HistogramView(histogramData = histogramData)
                    }
                    AnalysisTab.TELEMETRY -> {
                        TelemetryDiagnosticsView(
                            frameStats = frameStats,
                            stream = selectedStream,
                            status = connectionStatus
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WindowLevelControls(
    windowWidth: Float,
    windowCenter: Float,
    onWindowLevelChanged: (width: Float, center: Float) -> Unit,
    presets: List<WindowLevelPreset>,
    onApplyPreset: (WindowLevelPreset) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Presets Chips
        Text(
            text = "MEDICAL PRESETS",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MedicalCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = (windowWidth == preset.windowWidth && windowCenter == preset.windowCenter)
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) MedicalCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) MedicalCyan else DarkSurfaceBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onApplyPreset(preset) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) MedicalCyan else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Window Width Slider (Contrast)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "WINDOW WIDTH (Contrast Range)",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
            )
            Text(
                text = "%.0f".format(windowWidth),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MedicalCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            )
        }

        Slider(
            value = windowWidth,
            onValueChange = { onWindowLevelChanged(it, windowCenter) },
            valueRange = 10f..512f,
            colors = SliderDefaults.colors(
                thumbColor = MedicalCyan,
                activeTrackColor = MedicalCyan,
                inactiveTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.height(28.dp)
        )

        // Window Center Slider (Brightness / Level)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "WINDOW CENTER (Midpoint / Level)",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp)
            )
            Text(
                text = "%.0f".format(windowCenter),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MedicalCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            )
        }

        Slider(
            value = windowCenter,
            onValueChange = { onWindowLevelChanged(windowWidth, it) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = MedicalCyan,
                activeTrackColor = MedicalCyan,
                inactiveTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorMapControls(
    currentMode: ColorMapMode,
    onModeSelected: (ColorMapMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "PSEUDO-COLOR & RADIOLOGY LOOKUP TABLES (LUT)",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MedicalCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorMapMode.values().forEach { mode ->
                val isSelected = currentMode == mode
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) MedicalCyan.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.5.dp,
                            if (isSelected) MedicalCyan else DarkSurfaceBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onModeSelected(mode) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Color swatch preview
                        val previewGradient = when (mode) {
                            ColorMapMode.NATURAL -> Brush.horizontalGradient(listOf(Color.Red, Color.Green, Color.Blue))
                            ColorMapMode.GRAYSCALE -> Brush.horizontalGradient(listOf(Color.Black, Color.Gray, Color.White))
                            ColorMapMode.INVERTED_XRAY -> Brush.horizontalGradient(listOf(Color.White, Color.Gray, Color.Black))
                            ColorMapMode.THERMAL_JET -> Brush.horizontalGradient(listOf(Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red))
                            ColorMapMode.HOT_IRON -> Brush.horizontalGradient(listOf(Color.Black, Color.Red, Color.Yellow, Color.White))
                        }

                        Box(
                            modifier = Modifier
                                .size(width = 18.dp, height = 12.dp)
                                .background(previewGradient, RoundedCornerShape(2.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSelected) MedicalCyan else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistogramView(histogramData: IntArray) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REAL-TIME LUMINANCE HISTOGRAM (64 BINS)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MedicalCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            )

            val maxVal = histogramData.maxOrNull() ?: 1
            Text(
                text = "PEAK: $maxVal px",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Histogram Graph Canvas
        val maxCount = (histogramData.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(DarkCanvas, RoundedCornerShape(6.dp))
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(6.dp))
                .padding(4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height
                val binWidth = canvasW / histogramData.size

                for (i in histogramData.indices) {
                    val count = histogramData[i]
                    val barHeight = (count / maxCount) * canvasH
                    val x = i * binWidth

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(MedicalCyan, MedicalCyan.copy(alpha = 0.3f)),
                            startY = canvasH - barHeight,
                            endY = canvasH
                        ),
                        topLeft = Offset(x, canvasH - barHeight),
                        size = Size(binWidth - 1f, barHeight)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "0 (Shadows / Air)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
            Text(text = "128 (Soft Tissue)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
            Text(text = "255 (Highlights / Bone)", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
        }
    }
}

@Composable
private fun TelemetryDiagnosticsView(
    frameStats: FrameStats,
    stream: OmtStreamSource?,
    status: ConnectionStatus
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "OMT NETWORK DIAGNOSTICS & TRANSPORT",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MedicalCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TARGET: ${stream?.displayAddress ?: "None"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "STATUS: ${status.name}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (status == ConnectionStatus.CONNECTED) MedicalEmerald else MedicalAmber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "CODEC: ${frameStats.format}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FPS: %.1f".format(frameStats.fps),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MedicalEmerald,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "THROUGHPUT: %.2f Mbps".format(frameStats.bitrateMbps),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MedicalCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
                Text(
                    text = "JITTER/LATENCY: %.1f ms".format(frameStats.latencyMs),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tip: Make sure Open Camera OMT on your phone has 'OMT Streaming' enabled in Video Settings and is on the same local Wi-Fi subnet.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 10.sp)
        )
    }
}
