package com.nestmate.app.utils.verification

/**
 * EmailJS Dashboard Configuration Required:
 * ─────────────────────────────────────────────────────────────
 * Template "To Email" field  → {{user_email}}
 * Template body OTP variable → {{otp_code}}
 * Template name variable     → {{user_name}}
 * Template expiry variable   → {{expiry_minutes}}
 * Allowed Origins            → http://localhost  (for Android dev builds)
 *
 * To whitelist the origin:
 *   EmailJS Dashboard → Account → Security → Allowed Origins → Add "http://localhost"
 * ─────────────────────────────────────────────────────────────
 */
object EmailJsConfig {
    // Replace these placeholders with your actual EmailJS credentials
    const val SERVICE_ID  = "service_nykuc9v"
    const val TEMPLATE_ID = "template_o2uruvr"
    const val PUBLIC_KEY  = "gDYzTNJl-JwOZ7P7t"
    const val API_URL     = "https://api.emailjs.com/api/v1.0/email/send"

    /**
     * Must match one of the "Allowed Origins" configured in your EmailJS account dashboard.
     * Android's OkHttp does NOT send an Origin header automatically — so we must add it manually.
     * For production builds, replace with your actual whitelisted domain.
     */
    const val ORIGIN = "http://localhost"
}
