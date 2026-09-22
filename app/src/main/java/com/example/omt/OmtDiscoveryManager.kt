package com.example.omt

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.example.model.OmtSourceType
import com.example.model.OmtStreamSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class OmtDiscoveryManager(private val context: Context) {

    private val tag = "OmtDiscovery"
    private val nsdManager: NsdManager? = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager: WifiManager? = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    private val knownStreamsMap = ConcurrentHashMap<String, OmtStreamSource>()
    private val _discoveredStreams = MutableStateFlow<List<OmtStreamSource>>(emptyList())
    val discoveredStreams: StateFlow<List<OmtStreamSource>> = _discoveredStreams.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    init {
        initializeDefaultDemoSources()
    }

    private fun initializeDefaultDemoSources() {
        val demo1 = OmtStreamSource(
            id = "demo_ct_scan",
            name = "Live Medical CT: Angio / Thorax",
            host = "127.0.0.1",
            port = 6400,
            sourceType = OmtSourceType.DEMO_CT_SCAN,
            description = "Simulated high-res medical axial CT scan with respiratory cardiac dynamics",
            resolution = "1024x1024",
            estimatedFps = 30
        )
        val demo2 = OmtStreamSource(
            id = "demo_endoscopy",
            name = "Laparoscopic / Endoscopic Feed",
            host = "127.0.0.1",
            port = 6401,
            sourceType = OmtSourceType.DEMO_ENDOSCOPY,
            description = "Low-latency surgical endoscope camera feed simulation",
            resolution = "1920x1080",
            estimatedFps = 60
        )
        val demo3 = OmtStreamSource(
            id = "demo_calibration",
            name = "OMT Calibration / SMPTE Bars",
            host = "127.0.0.1",
            port = 6402,
            sourceType = OmtSourceType.DEMO_CALIBRATION,
            description = "Standard OMT SMPTE test pattern and frequency calibration target",
            resolution = "1920x1080",
            estimatedFps = 60
        )
        val localLoopback = OmtStreamSource(
            id = "local_phone_camera",
            name = "Local Phone Camera (Open Camera OMT)",
            host = "127.0.0.1",
            port = 6400,
            sourceType = OmtSourceType.LOCAL_LOOPBACK,
            description = "Connects to Open Camera with OMT enabled running on this same device via 127.0.0.1:6400",
            resolution = "Auto",
            estimatedFps = 30
        )

        knownStreamsMap[localLoopback.id] = localLoopback
        knownStreamsMap[demo1.id] = demo1
        knownStreamsMap[demo2.id] = demo2
        knownStreamsMap[demo3.id] = demo3
        updateStreamList()
    }

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true

        try {
            multicastLock = wifiManager?.createMulticastLock("OmtMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not acquire MulticastLock: ${e.message}")
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(tag, "OMT Service discovery started for $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(tag, "OMT Service found: ${service.serviceName}")
                resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(tag, "OMT Service lost: ${service.serviceName}")
                val key = "lan_${service.serviceName}"
                knownStreamsMap.remove(key)
                updateStreamList()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(tag, "OMT Discovery stopped: $serviceType")
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "OMT Discovery start failed: Error code $errorCode")
                _isScanning.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(tag, "OMT Discovery stop failed: Error code $errorCode")
                _isScanning.value = false
            }
        }

        try {
            // OMT protocol mDNS service type: _omt._tcp
            nsdManager?.discoverServices("_omt._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start NSD discovery: ${e.message}")
            _isScanning.value = false
        }
    }

    private fun resolveService(service: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(tag, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val host = serviceInfo.host?.hostAddress ?: return
                val port = serviceInfo.port
                val name = serviceInfo.serviceName ?: "OMT Camera Source"
                val id = "lan_${name}_${host}_$port"

                val source = OmtStreamSource(
                    id = id,
                    name = name,
                    host = host,
                    port = port,
                    sourceType = OmtSourceType.LAN_DISCOVERED,
                    description = "OMT Network Stream at $host:$port",
                    resolution = "Live 1080p",
                    estimatedFps = 30,
                    isOnline = true
                )
                knownStreamsMap[id] = source
                updateStreamList()
            }
        }

        try {
            nsdManager?.resolveService(service, resolveListener)
        } catch (e: Exception) {
            Log.e(tag, "Error invoking resolveService: ${e.message}")
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager?.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.e(tag, "Error stopping discovery: ${e.message}")
            }
            discoveryListener = null
        }
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            Log.w(tag, "Error releasing MulticastLock: ${e.message}")
        }
        _isScanning.value = false
    }

    fun addManualStream(name: String, host: String, port: Int): OmtStreamSource {
        val id = "manual_${host}_$port"
        val cleanName = name.ifBlank { "OMT Source ($host:$port)" }
        val source = OmtStreamSource(
            id = id,
            name = cleanName,
            host = host.trim(),
            port = port,
            sourceType = OmtSourceType.MANUAL_IP,
            description = "Custom OMT Endpoint at $host:$port",
            resolution = "Custom",
            estimatedFps = 30,
            isOnline = true
        )
        knownStreamsMap[id] = source
        updateStreamList()
        return source
    }

    private fun updateStreamList() {
        _discoveredStreams.value = knownStreamsMap.values.sortedWith(
            compareBy<OmtStreamSource> {
                // Keep Local Phone Camera, LAN, and Manual streams at the top, followed by demos
                when (it.sourceType) {
                    OmtSourceType.LOCAL_LOOPBACK -> 0
                    OmtSourceType.LAN_DISCOVERED -> 1
                    OmtSourceType.MANUAL_IP -> 2
                    else -> 3
                }
            }.thenBy { it.name }
        )
    }
}
