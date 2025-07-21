package com.example.tradeup

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initializeCloudinary()
    }

    private fun initializeCloudinary() {
        try {
            val config = hashMapOf<String, String>(
                "cloud_name" to "dovf2zc0u",
                "api_key" to "954889699447999",
                "api_secret" to "jePY1jqFFEM3pbAmBo0i9XuVgQo"
            )

            MediaManager.init(this, config)
            Log.d("MyApplication", "✅ Cloudinary initialized successfully")
        } catch (e: Exception) {
            Log.e("MyApplication", "❌ Failed to initialize Cloudinary", e)
        }
    }
}