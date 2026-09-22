package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ColorMapMode
import com.example.model.FrameStats
import com.example.model.OmtStreamSource
import com.example.model.SpotPixelData
import com.example.model.WindowLevelPreset
import com.example.omt.ConnectionStatus
import com.example.omt.MedicalImageProcessor
import com.example.omt.OmtDiscoveryManager
import com.example.omt.OmtStreamClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AnalysisTab(val label: String) {
    WINDOW_LEVEL("Window/Level"),
    COLOR_MAP("Color LUT"),
    HISTOGRAM("Histogram"),
    TELEMETRY("Network/HUD")
}

class OmtViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val discoveryManager = OmtDiscoveryManager(application)
    private val streamClient = OmtStreamClient(viewModelScope)

    val discoveredStreams: StateFlow<List<OmtStreamSource>> = discoveryManager.discoveredStreams
    val isScanning: StateFlow<Boolean> = discoveryManager.isScanning

    private val _selectedStream = MutableStateFlow<OmtStreamSource?>(null)
    val selectedStream: StateFlow<OmtStreamSource?> = _selectedStream.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus> = streamClient.connectionStatus
    val statusMessage: StateFlow<String> = streamClient.statusMessage
    val frameStats: StateFlow<FrameStats> = streamClient.frameStats

    // Display Bitmap (processed or raw)
    private val _displayBitmap = MutableStateFlow<Bitmap?>(null)
    val displayBitmap: StateFlow<Bitmap?> = _displayBitmap.asStateFlow()

    // Freeze / Pause
    private val _isFrozen = MutableStateFlow(false)
    val isFrozen: StateFlow<Boolean> = _isFrozen.asStateFlow()
    private var frozenSourceBitmap: Bitmap? = null

    // Window & Level
    private val _windowWidth = MutableStateFlow(256f)
    val windowWidth: StateFlow<Float> = _windowWidth.asStateFlow()

    private val _windowCenter = MutableStateFlow(128f)
    val windowCenter: StateFlow<Float> = _windowCenter.asStateFlow()

    // Color Lookup Mode
    private val _colorMapMode = MutableStateFlow(ColorMapMode.NATURAL)
    val colorMapMode: StateFlow<ColorMapMode> = _colorMapMode.asStateFlow()

    // Crosshair & Spot Inspection
    private val _crosshairNorm = MutableStateFlow<Offset?>(null)
    val crosshairNorm: StateFlow<Offset?> = _crosshairNorm.asStateFlow()

    private val _spotPixelData = MutableStateFlow<SpotPixelData?>(null)
    val spotPixelData: StateFlow<SpotPixelData?> = _spotPixelData.asStateFlow()

    // Histogram
    private val _histogramData = MutableStateFlow(IntArray(64))
    val histogramData: StateFlow<IntArray> = _histogramData.asStateFlow()

    // Navigation & Overlay
    private val _activeTab = MutableStateFlow(AnalysisTab.WINDOW_LEVEL)
    val activeTab: StateFlow<AnalysisTab> = _activeTab.asStateFlow()

    private val _showHud = MutableStateFlow(true)
    val showHud: StateFlow<Boolean> = _showHud.asStateFlow()

    // Viewport Zoom & Pan
    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    val presets = listOf(
        WindowLevelPreset("Full / Default", 256f, 128f),
        WindowLevelPreset("Bone / High Contrast", 90f, 190f),
        WindowLevelPreset("Soft Tissue", 140f, 110f),
        WindowLevelPreset("Lung Parenchyma", 180f, 65f)
    )

    init {
        // Start mDNS network discovery
        discoveryManager.startDiscovery()

        // Auto-select first stream (CT Scan demo) for instant viewability
        viewModelScope.launch {
            discoveryManager.discoveredStreams.collectLatest { streams ->
                if (_selectedStream.value == null && streams.isNotEmpty()) {
                    selectStream(streams.first())
                }
            }
        }

        // Process incoming frames from OMT client
        viewModelScope.launch(Dispatchers.Default) {
            streamClient.rawFrameBitmap.collectLatest { rawBitmap ->
                if (!_isFrozen.value && rawBitmap != null) {
                    processIncomingBitmap(rawBitmap)
                }
            }
        }
    }

    fun selectStream(source: OmtStreamSource) {
        _selectedStream.value = source
        resetZoomAndPan()
        _isFrozen.value = false
        streamClient.connect(source)
    }

    fun refreshDiscovery() {
        discoveryManager.startDiscovery()
    }

    fun addManualStream(name: String, host: String, port: Int) {
        val newSource = discoveryManager.addManualStream(name, host, port)
        selectStream(newSource)
    }

    private fun processIncomingBitmap(bitmap: Bitmap) {
        // Fast medical adjustments (LUT + Window/Level)
        val processed = if (_windowWidth.value == 256f && _windowCenter.value == 128f && _colorMapMode.value == ColorMapMode.NATURAL) {
            bitmap
        } else {
            MedicalImageProcessor.applyMedicalAdjustments(
                source = bitmap,
                windowWidth = _windowWidth.value,
                windowCenter = _windowCenter.value,
                colorMapMode = _colorMapMode.value
            )
        }

        _displayBitmap.value = processed

        // Update histogram (subsampled)
        _histogramData.value = MedicalImageProcessor.computeHistogram(processed, 64)

        // Update crosshair if placed
        _crosshairNorm.value?.let { norm ->
            _spotPixelData.value = MedicalImageProcessor.samplePixelSpot(processed, norm.x, norm.y)
        }
    }

    fun setWindowLevel(width: Float, center: Float) {
        _windowWidth.value = width.coerceIn(10f, 512f)
        _windowCenter.value = center.coerceIn(0f, 255f)
        reprocessCurrentFrame()
    }

    fun applyPreset(preset: WindowLevelPreset) {
        setWindowLevel(preset.windowWidth, preset.windowCenter)
    }

    fun setColorMapMode(mode: ColorMapMode) {
        _colorMapMode.value = mode
        reprocessCurrentFrame()
    }

    fun toggleFreeze() {
        if (_isFrozen.value) {
            _isFrozen.value = false
            frozenSourceBitmap = null
        } else {
            _displayBitmap.value?.let {
                _isFrozen.value = true
                frozenSourceBitmap = it
            }
        }
    }

    fun setCrosshair(normX: Float, normY: Float) {
        val clamped = Offset(normX.coerceIn(0f, 1f), normY.coerceIn(0f, 1f))
        _crosshairNorm.value = clamped
        _displayBitmap.value?.let { bmp ->
            _spotPixelData.value = MedicalImageProcessor.samplePixelSpot(bmp, clamped.x, clamped.y)
        }
    }

    fun clearCrosshair() {
        _crosshairNorm.value = null
        _spotPixelData.value = null
    }

    fun setAnalysisTab(tab: AnalysisTab) {
        _activeTab.value = tab
    }

    fun toggleHud() {
        _showHud.value = !_showHud.value
    }

    fun updateTransform(zoomDelta: Float, panChange: Offset) {
        val newZoom = (_zoomScale.value * zoomDelta).coerceIn(1f, 8f)
        _zoomScale.value = newZoom
        if (newZoom == 1f) {
            _panOffset.value = Offset.Zero
        } else {
            _panOffset.value = _panOffset.value + panChange
        }
    }

    fun resetZoomAndPan() {
        _zoomScale.value = 1f
        _panOffset.value = Offset.Zero
    }

    private fun reprocessCurrentFrame() {
        val base = if (_isFrozen.value) frozenSourceBitmap else streamClient.rawFrameBitmap.value
        base?.let { bmp ->
            viewModelScope.launch(Dispatchers.Default) {
                processIncomingBitmap(bmp)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        discoveryManager.stopDiscovery()
        streamClient.disconnect()
    }
}
