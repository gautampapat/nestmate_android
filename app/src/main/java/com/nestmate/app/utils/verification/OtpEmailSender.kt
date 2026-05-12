package com.nestmate.app.utils.verification

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object OtpEmailSender {
    private val client = OkHttpClient()

    suspend fun sendOtp(
        toEmail: String,
        userName: String,
        otpCode: String,
        expiryMinutes: Int = 10
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val templateParams = JSONObject().apply {
                put("user_email", toEmail)        // EmailJS template "To Email" → {{user_email}}
                put("user_name", userName)         // {{user_name}}
                put("otp_code", otpCode)           // {{otp_code}}
                put("expiry_minutes", expiryMinutes) // {{expiry_minutes}}
            }

            val payload = JSONObject().apply {
                put("service_id", EmailJsConfig.SERVICE_ID)
                put("template_id", EmailJsConfig.TEMPLATE_ID)
                put("user_id", EmailJsConfig.PUBLIC_KEY)
                put("template_params", templateParams)
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaType())

            // BUG FIX 2: Add Origin header — EmailJS validates it against allowed domains.
            // Android OkHttp does NOT send Origin automatically; omitting it causes 403 "Forbidden".
            val request = Request.Builder()
                .url(EmailJsConfig.API_URL)
                .post(requestBody)
                .addHeader("Origin", EmailJsConfig.ORIGIN)       // must match EmailJS dashboard allowlist
                .addHeader("Content-Type", "application/json")   // explicit, belt-and-suspenders
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    // BUG FIX 1: Read the body string BEFORE the use{} block closes the stream.
                    // Without this, the actual EmailJS error message (e.g. "Invalid template ID",
                    // "The recipients address is not configured") is swallowed completely.
                    val bodyString = response.body?.string() ?: "empty body"
                    Result.failure(Exception("EmailJS ${response.code}: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
