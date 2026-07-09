package com.niko.novaatlas

import kotlinx.serialization.Serializable

/**
 * Mapping exact du JSON renvoyé par GET http://192.168.1.22:5055/api/articles
 * Champs absents dans la réponse = null côté Kotlin (optionnel).
 */
@Serializable
data class Article(
    val hash: String,
    val timestamp: String,
    val category: String,
    val title: String,
    val title_original: String? = null,
    val link: String? = null,
    val source: String? = null,
    val pub_date: String? = null,
    val summary: String? = null,
)

/**
 * Envelope de la réponse API : {status, date, count, articles[]}
 */
@Serializable
data class ArticlesResponse(
    val status: String,
    val date: String,
    val count: Int,
    val articles: List<Article>,
)
