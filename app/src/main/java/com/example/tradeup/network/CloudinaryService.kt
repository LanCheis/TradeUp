package com.example.tradeup.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface CloudinaryService {
    @Multipart
    @POST("image/upload")
    fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("upload_preset") preset: RequestBody
    ): Call<CloudinaryUploadResponse>
}
