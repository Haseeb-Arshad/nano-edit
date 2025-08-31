package com.example.myapplication.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

data class EditAccepted(
    val job_id: String,
    val status: String,
    val estimated_cost_cents: Int
)

data class EditStatus(
    val job_id: String,
    val status: String,
    val progress: Int?,
    val result_url: String?,
    val error: String?
)

interface ApiService {
    @Multipart
    @POST("api/v1/edit")
    suspend fun uploadForEdit(
        @Part file: MultipartBody.Part,
        @Part("prompt") prompt: RequestBody,
        @Header("Authorization") bearer: String,
        @Part("client_request_id") clientRequestId: RequestBody? = null,
        @Part mask: MultipartBody.Part? = null
    ): Response<EditAccepted>

    @GET("api/v1/edit/{id}")
    suspend fun getEditStatus(
        @Path("id") id: String,
        @Header("Authorization") bearer: String
    ): Response<EditStatus>
}

