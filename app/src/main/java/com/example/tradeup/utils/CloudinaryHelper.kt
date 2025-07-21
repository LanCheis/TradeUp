package com.example.tradeup.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

object CloudinaryHelper {

    fun uploadListingImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val publicId = "listings/${System.currentTimeMillis()}"

        try {
            MediaManager.get().upload(imageUri)
                .unsigned("android_unsigned")
                .option("public_id", publicId)
                .option("folder", "tradeup/listings")
                .option("quality", "auto")
                .option("fetch_format", "auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        onProgress?.invoke(0)
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = ((bytes * 100) / totalBytes).toInt()
                        onProgress?.invoke(progress)
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["secure_url"] as? String
                        if (imageUrl != null) {
                            onSuccess(imageUrl)
                        } else {
                            onFailure("No URL returned from Cloudinary")
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        onFailure(error.description ?: "Upload failed")
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        // Handle retry
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            onFailure("Failed to start upload: ${e.message}")
        }
    }

    // ✅ NEW METHOD - Upload profile images
    fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val publicId = "profiles/$userId"

        try {
            MediaManager.get().upload(imageUri)
                .unsigned("android_unsigned")
                .option("public_id", publicId)
                .option("folder", "tradeup/profiles")
                .option("overwrite", false) // Don't overwrite since it's unsigned
                .option("transformation", listOf(
                    mapOf(
                        "width" to 400,
                        "height" to 400,
                        "crop" to "fill",
                        "gravity" to "face",
                        "quality" to "auto"
                    )
                ))
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        onProgress?.invoke(0)
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = ((bytes * 100) / totalBytes).toInt()
                        onProgress?.invoke(progress)
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["secure_url"] as? String
                        if (imageUrl != null) {
                            onSuccess(imageUrl)
                        } else {
                            onFailure("No URL returned from Cloudinary")
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        onFailure(error.description ?: "Upload failed")
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        // Auto retry
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            onFailure("Failed to start upload: ${e.message}")
        }
    }

    // Generate optimized URLs
    fun getOptimizedUrl(originalUrl: String, width: Int, height: Int, crop: String = "limit"): String {
        return if (originalUrl.contains("cloudinary.com")) {
            originalUrl.replace(
                "/upload/",
                "/upload/w_$width,h_$height,c_$crop,q_auto,f_auto/"
            )
        } else {
            originalUrl
        }
    }
}