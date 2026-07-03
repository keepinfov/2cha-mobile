package dev.yaul.twocha.ui.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * CameraX analyzer decoding QR codes with ZXing core.
 *
 * Deliberately the only place that knows the decode engine: swap this class
 * if a different decoder is ever needed. ZXing is pure Java (no Play
 * Services), which keeps the app working on degoogled devices and eligible
 * for F-Droid; terminal-rendered config QRs are large and high-contrast, well
 * within its comfort zone.
 *
 * Frames are throttled and decoding stops after the first hit until
 * [reset] — the caller decides whether to keep scanning (e.g. after an
 * invalid payload).
 */
class QrImageAnalyzer(
    private val onQrDecoded: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    @Volatile
    private var paused = false
    private var lastAttemptMs = 0L

    /** Resume scanning after a rejected payload. */
    fun reset() {
        paused = false
    }

    override fun analyze(image: ImageProxy) {
        image.use { proxy ->
            if (paused) return
            val now = System.currentTimeMillis()
            if (now - lastAttemptMs < THROTTLE_MS) return
            lastAttemptMs = now

            val text = decode(proxy) ?: return
            paused = true
            onQrDecoded(text)
        }
    }

    private fun decode(proxy: ImageProxy): String? {
        // Y plane = luminance, which is all ZXing needs
        val yPlane = proxy.planes[0]
        val yBytes = ByteArray(yPlane.buffer.remaining()).also { yPlane.buffer.get(it) }
        val source = PlanarYUVLuminanceSource(
            yBytes,
            yPlane.rowStride,
            proxy.height,
            0,
            0,
            proxy.width,
            proxy.height,
            false
        )
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: NotFoundException) {
            null
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    private companion object {
        const val THROTTLE_MS = 150L
    }
}
