package com.example.model

enum class OmtSourceType {
    LOCAL_LOOPBACK,
    LAN_DISCOVERED,
    MANUAL_IP,
    DEMO_CT_SCAN,
    DEMO_ENDOSCOPY,
    DEMO_CALIBRATION
}

data class OmtStreamSource(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val sourceType: OmtSourceType = OmtSourceType.LAN_DISCOVERED,
    val description: String = "",
    val resolution: String = "1920x1080",
    val estimatedFps: Int = 30,
    val isOnline: Boolean = true
) {
    val displayAddress: String
        get() = when (sourceType) {
            OmtSourceType.LOCAL_LOOPBACK -> "Local Phone Camera ($host:$port)"
            OmtSourceType.LAN_DISCOVERED, OmtSourceType.MANUAL_IP -> "$host:$port"
            else -> "Simulated OMT Stream"
        }
}
