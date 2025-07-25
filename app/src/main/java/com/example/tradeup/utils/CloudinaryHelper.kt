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
                "api_key" to "954889699447999",
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
            "overwrite" to true,
            "transformation" to "w_400,h_400,c_fill,g_face,q_auto,f_jpg" // ✅ FIXED: String format
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

    // 🔥 NEW: FR-2.1.1 - Upload item images with indexing for multiple photos
    suspend fun uploadItemImage(
        context: Context,
        imageUri: Uri,
        itemId: String,
        imageIndex: Int
    ): String? = suspendCancellableCoroutine { continuation ->

        if (!isInitialized) {
            initialize(context)
        }

        // Create unique public ID for each item image with index
        val publicId = "item_${itemId}_img_${imageIndex}"

        val uploadOptions = mapOf(
            "public_id" to publicId,
            "folder" to "tradeup_items",
            "resource_type" to "image",
            "overwrite" to true,
            "transformation" to "w_800,h_600,c_fill,q_auto,f_jpg" // Optimized for item listings
        )

        try {
            println("🔄 Starting item image upload: $publicId")

            MediaManager.get().upload(imageUri)
                .options(uploadOptions)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        println("📤 Item image upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes * 100 / totalBytes).toInt()
                        println("📊 Item image upload progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val imageUrl = resultData["secure_url"] as? String
                        println("✅ Item image upload successful: $imageUrl")
                        continuation.resume(imageUrl)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        println("❌ Item image upload error: ${error.description}")
                        continuation.resume(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        println("🔄 Item image upload rescheduled: ${error.description}")
                        continuation.resume(null)
                    }
                })
                .dispatch()

        } catch (e: Exception) {
            println("❌ Exception during item image upload: ${e.message}")
            continuation.resume(null)
        }
    }

    // 🔥 UPDATED: Upload listing images (kept for backward compatibility)
    suspend fun uploadListingImage(
        context: Context,
        imageUri: Uri,
        listingId: String
    ): String? {
        // Delegate to the new uploadItemImage method with index 0
        return uploadItemImage(context, imageUri, listingId, 0)
    }

    // 🔧 Get optimized image URL with transformations
    fun getOptimizedImageUrl(publicId: String, width: Int = 300, height: Int = 300): String {
        return "https://res.cloudinary.com/dovf2zc0u/image/upload/w_$width,h_$height,c_fill,g_face,q_auto,f_auto/$publicId"
    }

    // 🔥 NEW: Get optimized item image URL for different use cases
    fun getOptimizedItemImageUrl(
        itemId: String,
        imageIndex: Int,
        width: Int = 400,
        height: Int = 300
    ): String {
        val publicId = "tradeup_items/item_${itemId}_img_${imageIndex}"
        return "https://res.cloudinary.com/dovf2zc0u/image/upload/w_$width,h_$height,c_fill,q_auto,f_auto/$publicId"
    }

    // 🔥 NEW: Get thumbnail version for item grid views
    fun getItemThumbnailUrl(itemId: String, imageIndex: Int = 0): String {
        return getOptimizedItemImageUrl(itemId, imageIndex, 200, 150)
    }

    // 🔥 NEW: Get full-size version for item detail views
    fun getItemFullImageUrl(itemId: String, imageIndex: Int = 0): String {
        return getOptimizedItemImageUrl(itemId, imageIndex, 800, 600)
    }
}