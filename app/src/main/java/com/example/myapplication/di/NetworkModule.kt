package com.example.myapplication.di

import com.example.myapplication.BuildConfig
import com.example.myapplication.network.NanoBananaApi
import com.example.myapplication.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val auth = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.NB_API_KEY}")
                .build()
            chain.proceed(req)
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(auth)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        // Ensure baseUrl ends with a '/'; avoid runtime crash when misconfigured
        val raw = BuildConfig.NB_BASE_URL ?: ""
        val normalized = if (raw.endsWith('/')) raw else "$raw/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): NanoBananaApi = retrofit.create(NanoBananaApi::class.java)

    @Provides
    @Singleton
    fun provideBackendApi(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)
}
