package com.niko.novaatlas

import kotlinx.serialization.Serializable
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Serializable data class SiteCategory(val id: String, val name: String, val icon: String? = null, val order: Int = 0)
@Serializable data class SiteResponse(val status: String, val categories: List<SiteCategory>, val streams: Map<String,String>)
@Serializable data class Attribution(val author: String = "", val license: String = "", val source: String = "", val credit: String = "", val license_url: String = "", val changes: String = "")
@Serializable data class PublicImage(val url: String, val fallback: Boolean = true, val caption: String = "", val attribution: Attribution? = null)
@Serializable data class PublicArticle(val hash: String, val title: String, val category: String, val summary: String = "", val source: String = "", val link: String? = null, val original_title: String? = null, val published_raw: String = "", val published_at: String? = null, val observed_raw: String = "", val image: PublicImage? = null) {
    fun legacy() = Article(hash, observed_raw, category, title, original_title, link, source, published_raw, summary, image)
}
@Serializable data class PublicArticles(val status: String, val date: String, val total: Int, val articles: List<PublicArticle>)
@Serializable data class EventCoverage(val article_hash: String, val title: String, val category: String, val source: String = "", val link: String? = null, val published_raw: String = "", val published_at: String? = null)
@Serializable data class EditorialEvent(val event_id: String, val title: String, val category: String, val article_count: Int, val domain_count: Int, val articles: List<EventCoverage> = emptyList(), val last_updated_at: String? = null, val image: PublicImage? = null)
@Serializable data class EventsResponse(val status: String, val events: List<EditorialEvent>, val index_available: Boolean)
@Serializable data class EventResponse(val status: String, val event: EditorialEvent)

object SafeLinks {
    fun external(value: String?): String? = try {
        val uri = URI(value ?: "")
        if (uri.scheme in listOf("https", "http") && uri.host != null && uri.userInfo == null && value!!.none { it.code < 32 }) value else null
    } catch (_: Exception) { null }
    fun local(value: String?, prefix: String): String? = try {
        val uri = URI(value ?: "")
        if (value != null && value.startsWith(prefix) && !value.contains('%') && !value.contains('\\') && !value.contains("..") && uri.host == null && uri.scheme == null && uri.rawQuery == null && uri.rawFragment == null && value.none { it.code < 32 }) value else null
    } catch (_: Exception) { null }
    fun image(value: PublicImage?): String? = if (value?.fallback == false && value.attribution != null) local(value.url, "/media/") else null
    fun stream(value: String?): String? = local(value, "/stream/")
}
fun reliableInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try { OffsetDateTime.parse(value).toInstant() } catch (_: Exception) {
        try { ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() } catch (_: Exception) { null }
    }
}
