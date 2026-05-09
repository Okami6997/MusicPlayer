package com.musicplayer.data.remote.download

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadClient @Inject constructor() {

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Timber.d("Download API: $message")
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun createApi(baseUrl: String): DownloadApi {
        // Ensure the base URL ends with /api/ for the download service
        val formattedBaseUrl = when {
            baseUrl.endsWith("/api/") -> baseUrl
            baseUrl.endsWith("/api") -> "$baseUrl/"
            baseUrl.endsWith("/") -> "${baseUrl}api/"
            else -> "$baseUrl/api/"
        }
        Timber.d("Creating Download API with base URL: $formattedBaseUrl")
        return Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DownloadApi::class.java)
    }
}
