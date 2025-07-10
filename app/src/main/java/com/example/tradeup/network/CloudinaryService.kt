package com.example.tradeup.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface CloudinaryService {
    @Multipart
    @POST("v1_1/dovf2zc0u/image/upload")
    fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("upload_preset") preset: RequestBody
    ): Call<CloudinaryResponse> //very important line that doesn't make hell loose here
}
