package com.example.tradeup.utils

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object CloudinaryHelper {

    private var isInitialized = false

    // 🔧 Initialize Cloudinary - REPLACE WITH YOUR ACTUAL CREDENTIALS
    fun initialize(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to "dovf2zc0u",
                "api_key" to "123456789012345",
                "api_secret" to "jePY1jqFFEM3pbAmBo0i9XuVgQo"
            )

            try {
                MediaManager.init(context, config)
                isInitialized = true
                println("✅ Cloudinary initialized successfully")
            } catch (e: Exception) {
                println("❌ Cloudinary initialization failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // 🔧 Upload profile image to Cloudinary
    suspend fun uploadProfileImage(
        context: Context,
        imageUri: Uri,
        userId: String
    ): String? = suspendCancellableCoroutine { continuation ->

        if (!isInitialized) {
            initialize(context)
        }

        // Simplified upload options to avoid signature issues
        val publicId = "profile_$userId" // Simple public ID

        val uploadOptions = mapOf(
            "public_id" to publicId,
            "folder" to "tradeup_profiles",
            "resource_type" to "image",
            "overwrite" to true, // Allow overwriting existing images
            "transformation" to mapOf(
                "width" to 400,
                "height" to 400,
                "crop" to "fill",
                "gravity" to "face",
                "quality" to "auto",
                "format" to "jpg"
            )
        )

        try {
            println("🔄 Starting Cloudinary upload for user: $userId")

            MediaManager.get().upload(imageUri)
                .options(uploadOptions)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        println("📤 Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        println("📊 Upload progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["secure_url"] as? String
                        println("✅ Upload successful: $imageUrl")
                        continuation.resume(imageUrl)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        println("❌ Upload error: ${error.description}")
                        continuation.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        println("🔄 Upload rescheduled: ${error.description}")
                        continuation.resume(null)
                    }
                })
                .dispatch()

        } catch (e: Exception) {
            println("❌ Exception during upload: ${e.message}")
            continuation.resume(null)
        }
    }

    // 🔧 Get optimized image URL with transformations
    fun getOptimizedImageUrl(publicId: String, width: Int = 300, height: Int = 300): String {
        return "https://res.cloudinary.com/dovf2zc0u/image/upload/w_$width,h_$height,c_fill,g_face,q_auto,f_auto/$publicId"
    }
}