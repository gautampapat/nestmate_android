package com.nestmate.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ImageStorageUtil {

    /**
     * Opens the image at [uri], scales it to max 400px wide, compresses to JPEG at 60%,
     * and returns a Base64-encoded string (NO_WRAP, no newlines). Stored directly in Firestore.
     * No external service needed.
     */
    suspend fun compressToBase64(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(Exception("Cannot open image"))

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = false }
                val original = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                if (original == null) {
                    return@withContext Result.failure(Exception("Could not decode image"))
                }

                val scaled = if (original.width > 400) {
                    val ratio = 400.0f / original.width
                    val newHeight = (original.height * ratio).toInt()
                    Bitmap.createScaledBitmap(original, 400, newHeight, true)
                } else {
                    original
                }

                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 60, baos)
                val bytes = baos.toByteArray()

                // NO_WRAP: produces a single-line base64 string with no newlines — required for
                // clean storage in Firestore and clean decoding in BitmapFactory later.
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Result.success(base64)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Decodes a Base64 string back to a Bitmap for display in AsyncImage / Image composables.
     * Returns null if the string is blank or decode fails.
     */
    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        return try {
            if (base64.isBlank()) return null
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
