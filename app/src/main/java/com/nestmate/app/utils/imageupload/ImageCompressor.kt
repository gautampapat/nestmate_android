package com.nestmate.app.utils.imageupload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ImageCompressor {

    private const val MAX_DIMENSION = 1920
    private val qualityLadder = intArrayOf(85, 70, 55)

    suspend fun compress(
        uri: Uri,
        context: Context,
        targetMaxBytes: Long = 500_000L,
    ): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = decodeScaled(uri, context)
            ?: throw IllegalStateException("Unable to decode image at $uri")
        try {
            for (quality in qualityLadder) {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val bytes = out.toByteArray()
                if (bytes.size <= targetMaxBytes) return@withContext bytes
            }
            throw IllegalStateException(
                "Image could not be compressed below $targetMaxBytes bytes at quality 55",
            )
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Reads the URI into a [ByteArray] in a single stream pass, then decodes from that buffer.
     *
     * Why single-pass: Android Photo Picker URIs (content://media/picker/...) grant
     * single-use, non-repeatable read access — opening [ContentResolver.openInputStream]
     * twice on the same URI returns null or a corrupt stream on the second call, causing
     * [BitmapFactory.decodeStream] to return null.
     *
     * By buffering the raw bytes first, we can make as many [BitmapFactory.decodeByteArray]
     * calls as needed (once for bounds, once for pixels) without touching the ContentResolver again.
     */
    private fun decodeScaled(uri: Uri, context: Context): Bitmap? {
        // Step 1 — buffer the entire URI stream into memory (single ContentResolver read)
        val rawBytes: ByteArray = context.contentResolver.openInputStream(uri)
            ?.use { it.readBytes() }
            ?: return null

        if (rawBytes.isEmpty()) return null

        // Step 2 — first pass: read dimensions only (no pixel allocation)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Step 3 — second pass: decode pixels with calculated sub-sampling
        val opts = BitmapFactory.Options().apply {
            inSampleSize = inSampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampled = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)
            ?: return null

        // Step 4 — fine-scale to exactly MAX_DIMENSION if still oversized after sub-sampling
        val longest = maxOf(sampled.width, sampled.height)
        if (longest <= MAX_DIMENSION) return sampled

        val scale = MAX_DIMENSION.toFloat() / longest.toFloat()
        val targetW = (sampled.width * scale).toInt().coerceAtLeast(1)
        val targetH = (sampled.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(sampled, targetW, targetH, true)
        if (scaled !== sampled) sampled.recycle()
        return scaled
    }

    private fun inSampleSizeFor(width: Int, height: Int, targetDim: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while ((longest / sample) > targetDim * 2) sample *= 2
        return sample
    }
}
