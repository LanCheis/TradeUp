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

    // 🔧 Initialize Cloudinary - REPLACE WITH YOUR CREDENTIALS
    fun initialize(context: Context) {
        if (!isInitialized) {
            val config = mapOf(
                "cloud_name" to "your_cloud_name", // 🚨 REPLACE WITH YOUR CLOUDINARY CLOUD NAME
                "api_key" to "your_api_key",       // 🚨 REPLACE WITH YOUR API KEY
                "api_secret" to "your_api_secret"  // 🚨 REPLACE WITH YOUR API SECRET
            )

            try {
                MediaManager.init(context, config)
                isInitialized = true
            } catch (e: Exception) {
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

        val publicId = "profile_images/$userId" // Organized folder structure

        val uploadOptions = mapOf(
            "public_id" to publicId,
            "folder" to "tradeup_profiles",
            "resource_type" to "image",
            "transformation" to listOf(
                mapOf(
                    "width" to 400,
                    "height" to 400,
                    "crop" to "fill",
                    "gravity" to "face",
                    "quality" to "auto",
                    "format" to "jpg"
                )
            )
        )

        try {
            MediaManager.get().upload(imageUri)
                .options(uploadOptions)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        // Upload started
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        // Progress update (optional)
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["secure_url"] as? String
                        continuation.resume(imageUrl)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resume(null)
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    // 🔧 Get optimized image URL with transformations
    fun getOptimizedImageUrl(publicId: String, width: Int = 300, height: Int = 300): String {
        return "https://res.cloudinary.com/your_cloud_name/image/upload/w_$width,h_$height,c_fill,g_face,q_auto,f_auto/$publicId"
    }
}