package com.example.tradeup.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * FR-2.1.4: Helper class for uploading images to Cloudinary
 * Supports JPEG/PNG formats and handles up to 10 images per listing
 */
class CloudinaryHelper {

    companion object {
        private const val TAG = "CloudinaryHelper"
        private const val CLOUD_NAME = "your_cloud_name" // Replace with your Cloudinary cloud name
        private const val API_KEY = "your_api_key" // Replace with your API key
        private const val API_SECRET = "your_api_secret" // Replace with your API secret

        private var isInitialized = false

        fun initialize(context: Context) {
            if (!isInitialized) {
                try {
                    val config = hashMapOf(
                        "cloud_name" to CLOUD_NAME,
                        "api_key" to API_KEY,
                        "api_secret" to API_SECRET
                    )

                    MediaManager.init(context, config)
                    isInitialized = true
                    Log.d(TAG, "✅ Cloudinary initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to initialize Cloudinary: ${e.message}")
                }
            }
        }
    }

    /**
     * Upload an image to Cloudinary and return the URL
     * FR-2.1.4: Supports JPEG/PNG formats
     */
    suspend fun uploadImage(imageUri: Uri, context: Context): String? {
        if (!isInitialized) {
            initialize(context)
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val requestId = MediaManager.get().upload(imageUri)
                    .option("folder", "tradeup_listings") // Organize uploads in folders
                    .option("resource_type", "image")
                    .option("format", "jpg") // Convert to JPG for consistency
                    .option("quality", "auto:good") // Optimize quality
                    .option("fetch_format", "auto") // Auto-select best format
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d(TAG, "🚀 Upload started: $requestId")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = (bytes * 100 / totalBytes).toInt()
                            Log.d(TAG, "📊 Upload progress: $progress%")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val imageUrl = resultData["secure_url"] as? String
                            Log.d(TAG, "✅ Upload successful: $imageUrl")

                            if (continuation.isActive) {
                                continuation.resume(imageUrl)
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            Log.e(TAG, "❌ Upload failed: ${error.description}")

                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            Log.w(TAG, "⏰ Upload rescheduled: ${error.description}")
                        }
                    })
                    .dispatch()

                // Handle cancellation
                continuation.invokeOnCancellation {
                    try {
                        MediaManager.get().cancelRequest(requestId)
                        Log.d(TAG, "🚫 Upload cancelled: $requestId")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Failed to cancel upload: ${e.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Upload exception: ${e.message}")
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    /**
     * Upload multiple images and return list of URLs
     * FR-2.1.4: Supports up to 10 images
     */
    suspend fun uploadMultipleImages(imageUris: List<Uri>, context: Context): List<String> {
        val uploadedUrls = mutableListOf<String>()

        // Limit to 10 images as per FR-2.1.4
        val limitedUris = imageUris.take(10)

        for (uri in limitedUris) {
            val uploadedUrl = uploadImage(uri, context)
            uploadedUrl?.let { url ->
                uploadedUrls.add(url)
            }
        }

        Log.d(TAG, "✅ Uploaded ${uploadedUrls.size} out of ${limitedUris.size} images")
        return uploadedUrls
    }

    /**
     * Delete an image from Cloudinary using its public ID
     */
    suspend fun deleteImage(imageUrl: String): Boolean {
        return try {
            // Extract public ID from URL
            val publicId = extractPublicIdFromUrl(imageUrl)

            if (publicId != null) {
                // Note: Deletion requires server-side implementation for security
                // This is a placeholder for the deletion logic
                Log.d(TAG, "🗑️ Image deletion requested: $publicId")
                true
            } else {
                Log.w(TAG, "⚠️ Could not extract public ID from URL: $imageUrl")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting image: ${e.message}")
            false
        }
    }

    /**
     * Extract Cloudinary public ID from image URL
     */
    private fun extractPublicIdFromUrl(imageUrl: String): String? {
        return try {
            val urlParts = imageUrl.split("/")
            val uploadIndex = urlParts.indexOf("upload")

            if (uploadIndex != -1 && uploadIndex + 2 < urlParts.size) {
                // Get the part after /upload/version/
                val fileNameWithExtension = urlParts[uploadIndex + 2]
                // Remove file extension
                fileNameWithExtension.substringBeforeLast(".")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extracting public ID: ${e.message}")
            null
        }
    }

    /**
     * Get optimized image URL for different use cases
     */
    fun getOptimizedImageUrl(
        originalUrl: String,
        width: Int? = null,
        height: Int? = null,
        quality: String = "auto:good"
    ): String {
        return try {
            if (originalUrl.contains("cloudinary.com")) {
                var optimizedUrl = originalUrl

                // Add transformations
                val transformations = mutableListOf<String>()

                width?.let { w -> transformations.add("w_$w") }
                height?.let { h -> transformations.add("h_$h") }
                transformations.add("q_$quality")
                transformations.add("f_auto") // Auto format

                if (transformations.isNotEmpty()) {
                    val uploadIndex = optimizedUrl.indexOf("/upload/")
                    if (uploadIndex != -1) {
                        val before = optimizedUrl.substring(0, uploadIndex + 8)
                        val after = optimizedUrl.substring(uploadIndex + 8)
                        optimizedUrl = "$before${transformations.joinToString(",")}/$after"
                    }
                }

                optimizedUrl
            } else {
                originalUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error optimizing image URL: ${e.message}")
            originalUrl
        }
    }
}