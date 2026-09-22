package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.OmtViewerViewModel
import com.example.ui.components.ManualConnectDialog
import com.example.ui.components.OmtAnalysisPanel
import com.example.ui.components.OmtTopBar
import com.example.ui.components.OmtViewport
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                OmtViewerApp()
            }
        }
    }
}

@Composable
fun OmtViewerApp(
    viewModel: OmtViewerViewModel = viewModel()
) {
    var showManualDialog by remember { mutableStateOf(false) }

    val selectedStream by viewModel.selectedStream.collectAsState()
    val discoveredStreams by viewModel.discoveredStreams.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val frameStats by viewModel.frameStats.collectAsState()

    val displayBitmap by viewModel.displayBitmap.collectAsState()
    val isFrozen by viewModel.isFrozen.collectAsState()
    val showHud by viewModel.showHud.collectAsState()

    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()
    val crosshairNorm by viewModel.crosshairNorm.collectAsState()
    val spotPixelData by viewModel.spotPixelData.collectAsState()

    val windowWidth by viewModel.windowWidth.collectAsState()
    val windowCenter by viewModel.windowCenter.collectAsState()
    val colorMapMode by viewModel.colorMapMode.collectAsState()
    val histogramData by viewModel.histogramData.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            OmtTopBar(
                selectedStream = selectedStream,
                discoveredStreams = discoveredStreams,
                connectionStatus = connectionStatus,
                frameStats = frameStats,
                isScanning = isScanning,
                isFrozen = isFrozen,
                showHud = showHud,
                onSelectStream = { viewModel.selectStream(it) },
                onRefreshScan = { viewModel.refreshDiscovery() },
                onOpenManualDialog = { showManualDialog = true },
                onToggleFreeze = { viewModel.toggleFreeze() },
                onToggleHud = { viewModel.toggleHud() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkCanvas)
        ) {
            // Main Low-Latency Video Viewport (Takes remaining available height)
            OmtViewport(
                bitmap = displayBitmap,
                connectionStatus = connectionStatus,
                statusMessage = statusMessage,
                frameStats = frameStats,
                isFrozen = isFrozen,
                showHud = showHud,
                zoomScale = zoomScale,
                panOffset = panOffset,
                crosshairNorm = crosshairNorm,
                spotPixelData = spotPixelData,
                onTap = { x, y -> viewModel.setCrosshair(x, y) },
                onDoubleTap = { viewModel.resetZoomAndPan() },
                onTransform = { zoom, pan -> viewModel.updateTransform(zoom, pan) },
                onClearCrosshair = { viewModel.clearCrosshair() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Bottom Frame Analysis & Controls Panel
            OmtAnalysisPanel(
                activeTab = activeTab,
                onTabSelected = { viewModel.setAnalysisTab(it) },
                windowWidth = windowWidth,
                windowCenter = windowCenter,
                onWindowLevelChanged = { w, c -> viewModel.setWindowLevel(w, c) },
                presets = viewModel.presets,
                onApplyPreset = { viewModel.applyPreset(it) },
                colorMapMode = colorMapMode,
                onColorMapChanged = { viewModel.setColorMapMode(it) },
                histogramData = histogramData,
                frameStats = frameStats,
                selectedStream = selectedStream,
                connectionStatus = connectionStatus
            )
        }
    }

    if (showManualDialog) {
        ManualConnectDialog(
            onDismiss = { showManualDialog = false },
            onConnect = { name, host, port ->
                viewModel.addManualStream(name, host, port)
                showManualDialog = false
            }
        )
    }
}
