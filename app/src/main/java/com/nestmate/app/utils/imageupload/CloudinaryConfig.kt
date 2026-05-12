package com.nestmate.app.utils.imageupload

object CloudinaryConfig {
    // Replace these two values with your actual Cloudinary credentials before testing.
    // CLOUD_NAME  : visible on the Cloudinary Dashboard home page.
    // UPLOAD_PRESET: an UNSIGNED preset created at Dashboard → Settings → Upload → Upload Presets.
    const val CLOUD_NAME = "de9hzudw7"
    const val UPLOAD_PRESET = "nestmate_unsigned"

    // Constructed at runtime from CLOUD_NAME — never hardcode this directly.
    val BASE_URL get() = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
}
