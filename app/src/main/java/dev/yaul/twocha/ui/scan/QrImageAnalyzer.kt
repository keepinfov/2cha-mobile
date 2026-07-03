package dev.yaul.twocha.ui.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/**
 * CameraX analyzer decoding QR codes with ZXing core.
 *
 * Deliberately the only place that knows the decode engine: swap this class
 * if a different decoder is ever needed. ZXing is pure Java (no Play
 * Services), which keeps the app working on degoogled devices and eligible
 * for F-Droid.
 *
 * Tuned for the large, high-contrast QRs printed by `2cha setup`:
 *  - a QR-only [QRCodeReader] (no multi-format probing);
 *  - **no `TRY_HARDER`** — it multiplies work per frame and is pure overhead
 *    for these codes (this was the main cause of sluggish scanning);
 *  - the luminance is cropped to the centred square the user aims at, so we
 *    decode far fewer pixels and ignore background clutter;
 *  - each frame is tried both normally and inverted, so a code rendered
 *    light-on-dark (dark terminal themes) scans just as fast as dark-on-light.
 *
 * Decoding stops after the first hit until [reset] — the caller decides
 * whether to keep scanning (e.g. after an invalid payload).
 */
class QrImageAnalyzer(
    private val onQrDecoded: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = QRCodeReader()

    @Volatile
    private var paused = false

    /** Resume scanning after a rejected payload. */
    fun reset() {
        paused = false
    }

    override fun analyze(image: ImageProxy) {
        image.use { proxy ->
            if (paused) return
            val text = decode(proxy) ?: return
            paused = true
            onQrDecoded(text)
        }
    }

    private fun decode(proxy: ImageProxy): String? {
        val source = centeredLuminance(proxy)
        // Try the frame as-is, then inverted — cheap, and it makes the
        // decoder polarity-agnostic across terminal colour schemes.
        return decode(source) ?: decode(source.invert())
    }

    private fun decode(source: LuminanceSource): String? = try {
        reader.decode(BinaryBitmap(HybridBinarizer(source))).text
    } catch (_: ReaderException) {
        // NotFound / Checksum / Format — no code in this (sub)frame
        null
    } finally {
        reader.reset()
    }

    /** Y-plane luminance cropped to the centred square the scan frame shows. */
    private fun centeredLuminance(proxy: ImageProxy): PlanarYUVLuminanceSource {
        val yPlane = proxy.planes[0]
        val yBytes = ByteArray(yPlane.buffer.remaining()).also { yPlane.buffer.get(it) }

        val side = minOf(proxy.width, proxy.height)
        val left = (proxy.width - side) / 2
        val top = (proxy.height - side) / 2
        return PlanarYUVLuminanceSource(
            yBytes,
            yPlane.rowStride,
            proxy.height,
            left,
            top,
            side,
            side,
            false
        )
    }
}
