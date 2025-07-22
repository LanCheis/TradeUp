package com.example.tradeup.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

object CloudinaryHelper {

    private const val TAG = "CloudinaryHelper"

    fun uploadListingImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        onProgress: ((Int) -> Unit)? = null
    ) {
        val publicId = "listings_${System.currentTimeMillis()}"

        try {
            Log.d(TAG, "Starting listing image upload...")

            MediaManager.get().upload(imageUri)
                .unsigned("android_unsigned")
                .option("public_id", publicId)
                .option("folder", "tradeup/listings")
                .option("quality", "auto")
                .option("fetch_format", "auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Upload started: $requestId")
                        onProgress?.invoke(0)
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        if (totalBytes > 0) {
                            val progress = ((bytes * 100L) / totalBytes).toInt()
                            Log.d(TAG, "Upload progress: $progress%")
                            onProgress?.invoke(progress)
                        }
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        Log.d(TAG, "Upload successful: $requestId")
                        // ✅ Ensure we get the secure_url which will be our imageUrl
                        val imageUrl = resultData["secure_url"] as? String
                        if (imageUrl != null) {
                            onSuccess(imageUrl)
                        } else {
                            Log.e(TAG, "No URL returned from Cloudinary")
                            onFailure("No URL returned from Cloudinary")
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Upload error: ${error.description}")
                        onFailure(error.description ?: "Upload failed")
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start upload", e)
            onFailure("Failed to start upload: ${e.message}")
        }
    }

    fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        userId: String,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        onProgress: (Int) -> Unit
    ) {
        val publicId = "profile_${userId}_${System.currentTimeMillis()}"

        try {
            Log.d(TAG, "Starting profile image upload for user: $userId")

            MediaManager.get().upload(imageUri)
                .unsigned("android_unsigned") // Using same preset as listing images
                .option("public_id", publicId)
                .option("folder", "tradeup/profiles")
                .option("quality", "auto")
                .option("fetch_format", "auto")
                // Profile images should be square and reasonably sized
                .option("width", 400)
                .option("height", 400)
                .option("crop", "fill")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Profile upload started: $requestId")
                        onProgress(0)
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        if (totalBytes > 0) {
                            val progress = ((bytes * 100L) / totalBytes).toInt()
                            Log.d(TAG, "Profile upload progress: $progress%")
                            onProgress(progress)
                        }
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        Log.d(TAG, "Profile upload successful: $requestId")
                        val imageUrl = resultData["secure_url"] as? String
                        if (imageUrl != null) {
                            onSuccess(imageUrl)
                        } else {
                            Log.e(TAG, "No URL returned from Cloudinary for profile image")
                            onFailure("No URL returned from Cloudinary")
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "Profile upload error: ${error.description}")
                        onFailure(error.description ?: "Profile upload failed")
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "Profile upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start profile upload", e)
            onFailure("Failed to start profile upload: ${e.message}")
        }
    }

    fun getOptimizedUrl(originalUrl: String, width: Int, height: Int, crop: String = "limit"): String {
        return if (originalUrl.contains("cloudinary.com")) {
            val uploadIndex = originalUrl.indexOf("/upload/")
            if (uploadIndex != -1) {
                val beforeUpload = originalUrl.substring(0, uploadIndex)
                val afterUpload = originalUrl.substring(uploadIndex + "/upload/".length)
                "${beforeUpload}/upload/w_${width},h_${height},c_${crop},q_auto,f_auto/${afterUpload}"
            } else {
                originalUrl
            }
        } else {
            originalUrl
        }
    }

    fun isInitialized(): Boolean {
        return try {
            MediaManager.get() != null
            true
        } catch (e: Exception) {
            Log.e(TAG, "Cloudinary not initialized", e)
            false
        }
    }
}