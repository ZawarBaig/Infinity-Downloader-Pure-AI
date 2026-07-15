package com.example.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

data class CobaltRequest(
    val url: String,
    val videoQuality: String = "720", // "max", "1080", "720", "480", "360", "240", "144"
    val audioFormat: String = "mp3", // "mp3", "ogg", "wav", "opus"
    val audioOnly: Boolean = false,
    val downloadMode: String = "auto", // "auto", "video", "audio"
    val tiktokFullAudio: Boolean = false,
    val twitterGif: Boolean = true
)

data class CobaltPickerItem(
    val url: String,
    val type: String? = "video", // "video", "photo", "audio"
    val thumb: String? = null
)

data class CobaltResponse(
    val status: String, // "success", "redirect", "stream", "picker", "error"
    val url: String? = null,
    val text: String? = null, // error message
    val picker: List<CobaltPickerItem>? = null
)

interface CobaltApiService {
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST("/")
    suspend fun getDownloadUrl(@Body request: CobaltRequest): CobaltResponse

    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    @POST
    suspend fun getDownloadUrlCustomInstance(
        @Url customUrl: String,
        @Body request: CobaltRequest
    ): CobaltResponse
}

object CobaltClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val defaultOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun createService(baseUrl: String = "https://api.cobalt.tools/"): CobaltApiService {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(defaultOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CobaltApiService::class.java)
    }
}
