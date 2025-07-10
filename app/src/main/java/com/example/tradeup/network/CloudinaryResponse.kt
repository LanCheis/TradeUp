package com.example.tradeup.network

import com.google.gson.annotations.SerializedName

data class CloudinaryResponse(
    @SerializedName("secure_url")
    val secureUrl: String
)
