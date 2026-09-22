package com.example.model

import android.graphics.Bitmap

data class FrameStats(
    val fps: Float = 0f,
    val bitrateMbps: Float = 0f,
    val latencyMs: Float = 0f,
    val droppedFrames: Long = 0L,
    val totalFrames: Long = 0L,
    val format: String = "OMT-VMX",
    val resolution: String = "1920x1080"
)

data class SpotPixelData(
    val x: Int,
    val y: Int,
    val red: Int,
    val green: Int,
    val blue: Int,
    val luminance: Int,
    val estimatedHU: Int // Hounsfield Unit approximation for medical data
)

enum class ColorMapMode(val label: String) {
    NATURAL("True Color"),
    GRAYSCALE("Medical Mono"),
    INVERTED_XRAY("Inverse X-Ray"),
    THERMAL_JET("Thermal Jet"),
    HOT_IRON("Angio / Hot Iron")
}

data class WindowLevelPreset(
    val name: String,
    val windowWidth: Float,
    val windowCenter: Float
)
