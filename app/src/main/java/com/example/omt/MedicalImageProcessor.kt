package com.example.omt

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.ColorMapMode
import com.example.model.SpotPixelData
import kotlin.math.max
import kotlin.math.min

object MedicalImageProcessor {

    private val thermalJetLut = IntArray(256) { i ->
        val v = i / 255f
        val r = (1.5f - kotlin.math.abs(v * 4f - 3f)).coerceIn(0f, 1f)
        val g = (1.5f - kotlin.math.abs(v * 4f - 2f)).coerceIn(0f, 1f)
        val b = (1.5f - kotlin.math.abs(v * 4f - 1f)).coerceIn(0f, 1f)
        Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    private val hotIronLut = IntArray(256) { i ->
        val v = i / 255f
        val r = (v * 3f).coerceIn(0f, 1f)
        val g = ((v - 0.33f) * 3f).coerceIn(0f, 1f)
        val b = ((v - 0.66f) * 3f).coerceIn(0f, 1f)
        Color.rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    fun applyMedicalAdjustments(
        source: Bitmap,
        windowWidth: Float,
        windowCenter: Float,
        colorMapMode: ColorMapMode
    ): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Build 256-value transfer function for window/level
        val minVal = (windowCenter - windowWidth / 2f).coerceIn(0f, 255f)
        val maxVal = (windowCenter + windowWidth / 2f).coerceIn(0f, 255f)
        val span = if (maxVal > minVal) (maxVal - minVal) else 1f

        val transferLut = IntArray(256) { v ->
            val mapped = ((v - minVal) / span * 255f).toInt()
            mapped.coerceIn(0, 255)
        }

        val outputPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            // Calculate luminance
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
            val adjustedLum = transferLut[lum]

            val finalColor = when (colorMapMode) {
                ColorMapMode.NATURAL -> {
                    // Apply windowing to RGB components
                    val nr = transferLut[r]
                    val ng = transferLut[g]
                    val nb = transferLut[b]
                    Color.rgb(nr, ng, nb)
                }
                ColorMapMode.GRAYSCALE -> {
                    Color.rgb(adjustedLum, adjustedLum, adjustedLum)
                }
                ColorMapMode.INVERTED_XRAY -> {
                    val inv = 255 - adjustedLum
                    Color.rgb(inv, inv, inv)
                }
                ColorMapMode.THERMAL_JET -> {
                    thermalJetLut[adjustedLum]
                }
                ColorMapMode.HOT_IRON -> {
                    hotIronLut[adjustedLum]
                }
            }
            outputPixels[i] = finalColor
        }

        output.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return output
    }

    fun computeHistogram(bitmap: Bitmap, bins: Int = 64): IntArray {
        val histogram = IntArray(bins)
        val width = bitmap.width
        val height = bitmap.height
        val stepX = max(1, width / 120) // Subsample for real-time 60fps throughput
        val stepY = max(1, height / 120)

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
                val binIndex = (lum * bins / 256).coerceIn(0, bins - 1)
                histogram[binIndex]++
            }
        }
        return histogram
    }

    fun samplePixelSpot(bitmap: Bitmap, normX: Float, normY: Float): SpotPixelData? {
        if (normX !in 0f..1f || normY !in 0f..1f) return null
        val px = (normX * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val py = (normY * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)

        val color = bitmap.getPixel(px, py)
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)

        // Estimated Hounsfield Unit (Air = -1000, Water = 0, Soft Tissue = +40, Bone = +1000)
        // Scaled to typical 0-255 window representation:
        val estHU = (lum / 255f * 2000f - 1000f).toInt()

        return SpotPixelData(
            x = px,
            y = py,
            red = r,
            green = g,
            blue = b,
            luminance = lum,
            estimatedHU = estHU
        )
    }
}
