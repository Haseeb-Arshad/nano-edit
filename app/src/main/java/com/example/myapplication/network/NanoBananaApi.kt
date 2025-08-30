package com.example.myapplication.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class EditRequestDto(
    val imageUrl: String?,
    val maskUrl: String? = null,
    val prompt: String,
    val strength: Float? = null,
    val upscaling: Boolean = false,
    val size: String = "original"
)

data class EditJobDto(
    val id: String,
    val status: String,
    val resultUrl: String? = null,
    val error: String? = null
)

interface NanoBananaApi {
    @POST("/v1/edits")
    suspend fun createEdit(@Body body: EditRequestDto): EditJobDto

    @GET("/v1/jobs/{id}")
    suspend fun getJob(@Path("id") id: String): EditJobDto
}

