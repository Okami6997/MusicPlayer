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
        // The DownloadApi endpoints already include /api/ prefix (e.g., "api/search")
        // Strip any trailing /api/ or /api to avoid doubling
        var cleanedUrl = baseUrl.trimEnd('/')
        if (cleanedUrl.endsWith("/api")) {
            cleanedUrl = cleanedUrl.substring(0, cleanedUrl.length - 4) // Remove "/api"
        }
        val formattedBaseUrl = "$cleanedUrl/"
        Timber.d("Creating Download API with base URL: $formattedBaseUrl")
        return Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DownloadApi::class.java)
    }
}
