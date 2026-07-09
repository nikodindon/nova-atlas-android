package com.niko.novaatlas

import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

/**
 * Interface Retrofit unique : tous les endpoints Nova-Atlas consommes par l'app.
 * Pour l'instant : articles (public) + subscription status (premium check).
 */
interface NovaAtlasApi {
    @GET("api/articles")
    suspend fun getArticles(
        @Query("date") date: String? = null,      // YYYYMMDD, null = aujourd'hui
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 50,
    ): ArticlesResponse

    @GET("api/subscription/status")
    suspend fun getSubscriptionStatus(
        @Query("device_id") deviceId: String,
    ): SubscriptionStatusResponse
}

/**
 * Singleton Retrofit. On garde une seule instance pour tout le process.
 * - baseUrl = http://192.168.1.22:5055/
 * - JSON parser tolerant (ignoreUnknownKeys = true) : si le serveur ajoute
 *   un champ, l'app ne crash pas
 */
object ApiClient {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Construit l'URL de base selon le contexte reseau :
     * - LAN (chez toi, 192.168.1.22) si NetworkDiscovery a detecte le LAN
     * - Public (Cloudflare Tunnel) sinon (4G, autre WiFi, etc.)
     */
    private fun baseUrl(): String = if (NetworkDiscovery.useLocal.value) {
        "http://${BuildConfig.LAN_HOST}:${BuildConfig.LAN_PORT_FLASK}/"
    } else {
        "https://${BuildConfig.PUBLIC_HOST}:${BuildConfig.PUBLIC_PORT_FLASK}/"
    }

    val api: NovaAtlasApi = Retrofit.Builder()
        .baseUrl(baseUrl())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(NovaAtlasApi::class.java)
}

/**
 * Envelope de la reponse subscription (device_id, is_premium, expires_at, source).
 */
@kotlinx.serialization.Serializable
data class SubscriptionStatusResponse(
    val status: String,
    val device_id: String,
    val is_premium: Boolean,
    val expires_at: String? = null,
    val source: String? = null,
)
