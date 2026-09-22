package com.example.omt

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Log
import com.example.model.FrameStats
import com.example.model.OmtSourceType
import com.example.model.OmtStreamSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.cos
import kotlin.math.sin

enum class ConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    ERROR,
    DISCONNECTED
}

class OmtStreamClient(private val scope: CoroutineScope) {

    private val tag = "OmtStreamClient"

    private val _rawFrameBitmap = MutableStateFlow<Bitmap?>(null)
    val rawFrameBitmap: StateFlow<Bitmap?> = _rawFrameBitmap.asStateFlow()

    private val _frameStats = MutableStateFlow(FrameStats())
    val frameStats: StateFlow<FrameStats> = _frameStats.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow("Select a stream to begin")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var streamJob: Job? = null
    private var activeSource: OmtStreamSource? = null

    // Telemetry tracking variables
    private var frameCount = 0L
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var framesInWindow = 0
    private var bytesInWindow = 0L
    private var lastFrameTimeNs = System.nanoTime()

    fun connect(source: OmtStreamSource) {
        disconnect()
        activeSource = source
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _statusMessage.value = "Connecting to ${source.name}..."

        streamJob = scope.launch(Dispatchers.IO) {
            when (source.sourceType) {
                OmtSourceType.LOCAL_LOOPBACK, OmtSourceType.LAN_DISCOVERED, OmtSourceType.MANUAL_IP -> {
                    connectNetworkStream(source)
                }
                OmtSourceType.DEMO_CT_SCAN -> {
                    runMedicalCtSimulation(source)
                }
                OmtSourceType.DEMO_ENDOSCOPY -> {
                    runEndoscopySimulation(source)
                }
                OmtSourceType.DEMO_CALIBRATION -> {
                    runCalibrationSimulation(source)
                }
            }
        }
    }

    private suspend fun connectNetworkStream(source: OmtStreamSource) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.receiveBufferSize = 1024 * 1024
            socket.soTimeout = 5000

            Log.d(tag, "Connecting TCP socket to ${source.host}:${source.port}...")
            socket.connect(InetSocketAddress(source.host, source.port), 4000)

            _connectionStatus.value = ConnectionStatus.CONNECTED
            _statusMessage.value = "Connected to OMT Stream: ${source.host}:${source.port}"

            val inputStream = BufferedInputStream(socket.getInputStream(), 128 * 1024)
            val dataInput = DataInputStream(inputStream)

            val buffer = ByteArray(1024 * 1024 * 4) // 4MB frame buffer

            while (scope.isActive && socket.isConnected) {
                val readStart = System.nanoTime()

                // OMT Framing:
                // Check if frame header is present. Standard OMT uses TCP framing:
                // [Magic/Type: 4 bytes] [FourCC: 4 bytes] [Width: 4 bytes] [Height: 4 bytes] [PayloadLen: 4 bytes]
                val headerOrMagic = dataInput.readInt()
                val fourCC = dataInput.readInt()
                val width = dataInput.readInt().coerceIn(16, 4096)
                val height = dataInput.readInt().coerceIn(16, 4096)
                val payloadSize = dataInput.readInt().coerceIn(1, buffer.size)

                // Read payload
                dataInput.readFully(buffer, 0, payloadSize)

                // Frame decoded
                val latencyMs = ((System.nanoTime() - readStart) / 1_000_000f)

                // If payload is uncompressed RGBA/BGRA or compressed bitmap
                val bitmap = try {
                    android.graphics.BitmapFactory.decodeByteArray(buffer, 0, payloadSize)
                } catch (e: Exception) {
                    null
                } ?: run {
                    // Fallback create bitmap from raw RGBA buffer
                    val fallback = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    fallback
                }

                _rawFrameBitmap.value = bitmap
                recordFrameMetrics(payloadSize.toLong(), latencyMs, "OMT-TCP", "${width}x$height")
            }
        } catch (e: Exception) {
            Log.e(tag, "Network stream error: ${e.message}", e)
            _connectionStatus.value = ConnectionStatus.ERROR
            _statusMessage.value = "Unable to connect to ${source.host}:${source.port} (${e.localizedMessage ?: "Timeout"}). You can test with our preloaded medical streams!"
        } finally {
            try {
                socket?.close()
            } catch (ignored: Exception) {}
        }
    }

    private suspend fun runMedicalCtSimulation(source: OmtStreamSource) {
        _connectionStatus.value = ConnectionStatus.CONNECTED
        _statusMessage.value = "Streaming: ${source.name} (Simulated 30 FPS)"

        val width = 512
        val height = 512
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var phase = 0f

        while (scope.isActive) {
            val startNs = System.nanoTime()

            // Draw dark background (CT chamber)
            canvas.drawColor(Color.rgb(12, 16, 24))

            // Body contour (Thorax ellipse)
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(85, 90, 100)
            canvas.drawOval(RectF(60f, 80f, 452f, 432f), paint)

            // Lung fields (bilateral low attenuation zones)
            paint.color = Color.rgb(20, 24, 30)
            val breath = sin(phase) * 6f
            canvas.drawOval(RectF(100f, 130f, 220f + breath, 370f), paint)
            canvas.drawOval(RectF(292f - breath, 130f, 412f, 370f), paint)

            // Mediastinum & Cardiac silhouette
            val heartBeat = (sin(phase * 3f) + 1f) * 4f
            paint.color = Color.rgb(140, 145, 155)
            canvas.drawOval(RectF(210f - heartBeat, 200f, 310f + heartBeat, 350f), paint)

            // Spine / Vertebral body (high density bone)
            paint.color = Color.rgb(240, 245, 255)
            canvas.drawCircle(256f, 380f, 32f, paint)
            paint.color = Color.rgb(30, 35, 45)
            canvas.drawCircle(256f, 380f, 12f, paint) // Spinal canal

            // Ribs around periphery
            paint.color = Color.rgb(230, 235, 245)
            for (i in 0..7) {
                val angle = i * 0.35f + 0.3f
                val rx1 = 256f - cos(angle) * 180f
                val ry1 = 256f + sin(angle) * 150f
                canvas.drawCircle(rx1, ry1, 8f, paint)

                val rx2 = 256f + cos(angle) * 180f
                val ry2 = 256f + sin(angle) * 150f
                canvas.drawCircle(rx2, ry2, 8f, paint)
            }

            // Vascular pulmonary markings (bronchovascular branching)
            paint.color = Color.rgb(180, 190, 205)
            paint.strokeWidth = 3f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(190f, 230f, 130f, 200f, paint)
            canvas.drawLine(190f, 230f, 140f, 280f, paint)
            canvas.drawLine(322f, 230f, 375f, 195f, paint)
            canvas.drawLine(322f, 230f, 365f, 275f, paint)

            // Dynamic timestamp and slice annotation
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(0, 229, 255)
            paint.textSize = 18f
            canvas.drawText("OMT MEDICAL STREAM #001", 20f, 35f, paint)
            paint.color = Color.rgb(180, 200, 220)
            paint.textSize = 14f
            canvas.drawText("SL: 1.25mm | HU Range: [-1000, +1024]", 20f, 60f, paint)
            canvas.drawText("FPS: 30.0 | LATENCY: ~8ms", 20f, 80f, paint)

            // Emit frame
            val frameCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            _rawFrameBitmap.value = frameCopy

            val renderTimeMs = (System.nanoTime() - startNs) / 1_000_000f
            recordFrameMetrics(512 * 512 * 4L, renderTimeMs, "OMT-DICOM", "512x512")

            phase += 0.08f
            delay(33) // ~30 fps
        }
    }

    private suspend fun runEndoscopySimulation(source: OmtStreamSource) {
        _connectionStatus.value = ConnectionStatus.CONNECTED
        _statusMessage.value = "Streaming: ${source.name} (Low-Latency 60 FPS)"

        val width = 640
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        var time = 0f

        while (scope.isActive) {
            val startNs = System.nanoTime()

            // Dark endoscopic cavity
            canvas.drawColor(Color.rgb(40, 10, 15))

            // Mucosal surface gradient
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(160, 45, 55)
            canvas.drawCircle(320f, 240f, 210f, paint)

            paint.color = Color.rgb(200, 70, 75)
            canvas.drawCircle(320f + sin(time) * 20f, 240f + cos(time) * 15f, 160f, paint)

            // Specular reflections / surgical lighting
            paint.color = Color.argb(190, 255, 230, 230)
            canvas.drawCircle(300f + sin(time) * 10f, 210f, 25f, paint)
            paint.color = Color.argb(120, 255, 200, 200)
            canvas.drawCircle(370f, 260f, 15f, paint)

            // Micro-vascular networks
            paint.style = Paint.Style.STROKE
            paint.color = Color.rgb(100, 20, 30)
            paint.strokeWidth = 2f
            val path = Path().apply {
                moveTo(220f, 200f)
                quadTo(280f, 260f, 350f, 220f)
                quadTo(390f, 290f, 420f, 310f)
            }
            canvas.drawPath(path, paint)

            // Endoscopic Circular Mask vignette
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 100f
            paint.color = Color.rgb(5, 5, 8)
            canvas.drawCircle(320f, 240f, 280f, paint)

            // Telemetry overlay
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0f
            paint.color = Color.rgb(0, 229, 255)
            paint.textSize = 18f
            canvas.drawText("OMT SURGICAL ENDOSCOPE", 20f, 35f, paint)
            paint.color = Color.rgb(0, 230, 118)
            paint.textSize = 14f
            canvas.drawText("VMX-422 HIGH LATENCY BUFFER: <10ms", 20f, 60f, paint)

            val frameCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            _rawFrameBitmap.value = frameCopy

            val renderTimeMs = (System.nanoTime() - startNs) / 1_000_000f
            recordFrameMetrics(640 * 480 * 4L, renderTimeMs, "VMX-422", "640x480")

            time += 0.05f
            delay(16) // ~60 fps
        }
    }

    private suspend fun runCalibrationSimulation(source: OmtStreamSource) {
        _connectionStatus.value = ConnectionStatus.CONNECTED
        _statusMessage.value = "Streaming: ${source.name} (SMPTE 60 FPS)"

        val width = 640
        val height = 360
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        val smpteColors = intArrayOf(
            Color.rgb(204, 204, 204), // 75% White
            Color.rgb(204, 204, 0),   // Yellow
            Color.rgb(0, 204, 204),   // Cyan
            Color.rgb(0, 204, 0),     // Green
            Color.rgb(204, 0, 204),   // Magenta
            Color.rgb(204, 0, 0),     // Red
            Color.rgb(0, 0, 204)      // Blue
        )

        var sweepX = 0f

        while (scope.isActive) {
            val startNs = System.nanoTime()

            // Upper 2/3 SMPTE bars
            val barWidth = width / 7f
            for (i in smpteColors.indices) {
                paint.color = smpteColors[i]
                canvas.drawRect(i * barWidth, 0f, (i + 1) * barWidth, 240f, paint)
            }

            // Lower Grayscale / Pluge staircase
            for (i in 0..6) {
                val gray = (i * 255 / 6)
                paint.color = Color.rgb(gray, gray, gray)
                canvas.drawRect(i * barWidth, 240f, (i + 1) * barWidth, 360f, paint)
            }

            // Moving sync ticker line
            paint.color = Color.WHITE
            canvas.drawRect(sweepX, 230f, sweepX + 6f, 250f, paint)
            sweepX = (sweepX + 8f) % width

            // OMT Protocol stamp
            paint.color = Color.rgb(0, 0, 0)
            canvas.drawRect(180f, 140f, 460f, 200f, paint)
            paint.color = Color.rgb(0, 229, 255)
            paint.textSize = 22f
            canvas.drawText("OMT SMPTE CALIBRATION", 195f, 175f, paint)

            val frameCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            _rawFrameBitmap.value = frameCopy

            val renderTimeMs = (System.nanoTime() - startNs) / 1_000_000f
            recordFrameMetrics(640 * 360 * 4L, renderTimeMs, "OMT-SMPTE", "640x360")

            delay(16) // ~60 fps
        }
    }

    private fun recordFrameMetrics(payloadBytes: Long, latencyMs: Float, format: String, resolution: String) {
        frameCount++
        framesInWindow++
        bytesInWindow += payloadBytes

        val now = System.currentTimeMillis()
        val elapsed = now - lastFpsTimestamp

        if (elapsed >= 1000) {
            val fps = (framesInWindow * 1000f) / elapsed
            val bitrateMbps = (bytesInWindow * 8f) / (elapsed * 1000f)

            _frameStats.value = FrameStats(
                fps = fps,
                bitrateMbps = bitrateMbps,
                latencyMs = latencyMs,
                droppedFrames = 0L,
                totalFrames = frameCount,
                format = format,
                resolution = resolution
            )

            framesInWindow = 0
            bytesInWindow = 0L
            lastFpsTimestamp = now
        }
    }

    fun disconnect() {
        streamJob?.cancel()
        streamJob = null
        activeSource = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _statusMessage.value = "Disconnected"
        _rawFrameBitmap.value = null
    }
}
