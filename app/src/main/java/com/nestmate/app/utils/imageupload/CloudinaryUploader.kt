package com.nestmate.app.utils.imageupload

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads a compressed image [ByteArray] to Cloudinary using an unsigned upload preset.
 *
 * No secret key is required — unsigned presets are designed for client-side use.
 * The returned [Result] contains the permanent `secure_url` (HTTPS) that should be
 * stored in Firestore and loaded via Coil.
 *
 * Replaces the old Firebase-Storage-backed [ImageUploader]. Call sites only need to:
 *   1. Swap the injected type from `ImageUploader` to `CloudinaryUploader`.
 *   2. Replace the `storagePath` String parameter with a `folderHint` String
 *      in the format `"nestmate/{module}/{userId}"`.
 *
 * Progress reporting: Cloudinary's REST API does not support streaming progress.
 * Show an indeterminate indicator in the UI while [uploadCompressed] is running.
 * ViewModels should expose `isUploading: StateFlow<Boolean>` for this purpose.
 */
@Singleton
class CloudinaryUploader @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {

    /**
     * @param bytes       Compressed JPEG bytes (produced by [ImageCompressor]).
     * @param folderHint  Cloudinary folder path, e.g. `"nestmate/marketplace/uid123"`.
     *                    Cloudinary auto-generates the public_id inside this folder.
     * @return [Result.success] with the `secure_url` string on HTTP 200,
     *         [Result.failure] on any non-200 or network error.
     */
    suspend fun uploadCompressed(
        bytes: ByteArray,
        folderHint: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Base64-encode the bytes (NO_WRAP = single line, no newlines)
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$encoded"

            // 2. Build the multipart body
            val textType = "text/plain".toMediaType()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", dataUri)
                .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                .addFormDataPart("folder", folderHint)
                .build()

            // 3. Execute synchronous POST (we're already on Dispatchers.IO)
            val request = Request.Builder()
                .url(CloudinaryConfig.BASE_URL)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()

            // 4. Parse response
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from Cloudinary")

            if (!response.isSuccessful) {
                throw Exception("Cloudinary upload failed: HTTP ${response.code} — $responseBody")
            }

            val json = JSONObject(responseBody)
            json.getString("secure_url")
        }
    }
}
