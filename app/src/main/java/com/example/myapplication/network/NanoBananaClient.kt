package com.example.myapplication.network

import com.example.myapplication.BuildConfig
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NanoBananaClient {
    private val authInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder().apply {
            if (BuildConfig.NB_API_KEY.isNotEmpty()) {
                addHeader("Authorization", "Bearer ${BuildConfig.NB_API_KEY}")
            }
        }.build()
        chain.proceed(req)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.NB_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
    }

    val api: NanoBananaApi by lazy { retrofit.create(NanoBananaApi::class.java) }
}

