package com.niko.novaatlas

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit

interface NovaAtlasApi {
    @GET("api/articles") suspend fun getArticles(@Query("date") date: String? = null, @Query("category") category: String? = null, @Query("limit") limit: Int = 50): ArticlesResponse
    @GET("api/subscription/status") suspend fun getSubscriptionStatus(@Query("device_id") deviceId: String): SubscriptionStatusResponse
    @GET("api/public/site") suspend fun site(): SiteResponse
    @GET("api/public/articles") suspend fun articles(@Query("per_page") size: Int = 50): PublicArticles
    @GET("api/public/events") suspend fun events(@Query("per_page") size: Int = 20): EventsResponse
    @GET("api/public/events/{id}") suspend fun event(@Path("id") id: String): EventResponse
}

object ApiClient {
    val json = Json { ignoreUnknownKeys = true }
    // No URL/body logging, credential/cookie persistence, transparent retries or redirects.
    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code == 304 || !response.isSuccessful) return@addInterceptor response
            val body = response.body ?: return@addInterceptor response
            try {
                if (body.contentLength() > 2 * 1024 * 1024) throw IOException("response_limit")
                val source = body.source(); source.request(2 * 1024 * 1024L + 1)
                if (source.buffer.size > 2 * 1024 * 1024) throw IOException("response_limit")
                response
            } catch (e: Exception) { response.close(); throw e }
        }.build()
    fun create(origin: String, client: OkHttpClient = client()): NovaAtlasApi = Retrofit.Builder()
        .baseUrl(origin).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(NovaAtlasApi::class.java)
    val api: NovaAtlasApi by lazy { create(BuildConfig.API_ORIGIN) }
}

@Serializable data class SubscriptionStatusResponse(val status: String, val device_id: String, val is_premium: Boolean, val expires_at: String? = null, val source: String? = null)
