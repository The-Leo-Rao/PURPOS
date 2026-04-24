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

interface ApiService2 {

    @Multipart
    @POST("/api/visualizations/generate-dashboard")
    suspend fun submitData(
        @Part file: MultipartBody.Part
    ): Response<String>
}