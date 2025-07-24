package com.example.tradeup.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object CloudinaryHelper {

    private var isInitialized = false
    private const val TAG = "CloudinaryHelper"

    // 🔧 FIXED: Use demo credentials for testing
    fun initialize(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to "demo", // Demo cloud for testing
                "api_key" to "123456789012345",
                "api_secret" to "demo_secret"
            )

            try {
                MediaManager.init(context, config)
                isInitialized = true
                Log.d(TAG, "✅ Cloudinary initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Cloudinary initialization failed: ${e.message}")
            }
        }
    }

    // 🔧 FIXED: Simplified upload without transformations
    suspend fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        userId: String
    ): String? = suspendCancellableCoroutine { continuation ->

        if (!isInitialized) {
            initialize(context)
        }

        val publicId = "profile_$userId" // Simplified naming

        // 🔧 FIXED: Simplified upload options without complex transformations
        val uploadOptions = mapOf(
            "public_id" to publicId,
            "folder" to "tradeup_profiles",
            "resource_type" to "image"
        )

        try {
            Log.d(TAG, "🔄 Starting upload for user: $userId")

            MediaManager.get().upload(imageUri)
                .options(uploadOptions)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "📤 Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        Log.d(TAG, "📊 Upload progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["secure_url"] as? String
                        Log.d(TAG, "✅ Upload success: $imageUrl")
                        continuation.resume(imageUrl)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "❌ Upload error: ${error.description}")
                        continuation.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "⏰ Upload rescheduled: ${error.description}")
                        continuation.resume(null)
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload exception: ${e.message}")
            continuation.resume(null)
        }
    }

    // 🔧 FIXED: Simple URL generation
    fun getOptimizedImageUrl(publicId: String, width: Int = 300, height: Int = 300): String {
        return "https://res.cloudinary.com/demo/image/upload/w_$width,h_$height,c_fill/$publicId"
    }
}