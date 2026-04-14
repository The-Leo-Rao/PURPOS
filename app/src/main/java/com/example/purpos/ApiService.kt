package com.example.purpos

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("api/extract")
    suspend fun extractData(
        @Part image: MultipartBody.Part,
        @Part reference_file: MultipartBody.Part
    ): Response<String>
}