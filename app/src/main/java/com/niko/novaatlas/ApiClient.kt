package com.niko.novaatlas

import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

/**
 * Interface Retrofit qui décrit les endpoints Nova-Atlas consommés par l'app.
 * Pour l'instant: /api/articles avec filtres optionnels.
 */
interface NovaAtlasApi {
    @GET("api/articles")
    suspend fun getArticles(
        @Query("date") date: String? = null,      // YYYYMMDD, null = aujourd'hui
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 50,
    ): ArticlesResponse
}

/**
 * Singleton Retrofit. On garde une seule instance pour tout le process.
 * - baseUrl = http://192.168.1.22:5055/
 * - JSON parser tolérant (ignoreUnknownKeys = true) : si le serveur ajoute
 *   un champ, l'app ne crash pas
 */
object ApiClient {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val api: NovaAtlasApi = Retrofit.Builder()
        .baseUrl("http://192.168.1.22:5055/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(NovaAtlasApi::class.java)
}
