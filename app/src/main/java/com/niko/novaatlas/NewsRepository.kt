package com.niko.novaatlas

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import retrofit2.HttpException
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.math.max

class ApiFailure(val kind: String, val retrySeconds: Long = 60): Exception(kind)
fun retryAfter(value: String?, now: Long): Long? = value?.toLongOrNull()?.takeIf { it >= 0 }
    ?: reliableInstant(value)?.let { max(0, (it.toEpochMilli() - now + 999) / 1000) }
data class FeedState(val articles: List<Article> = emptyList(), val categories: List<SiteCategory> = emptyList(), val refreshing: Boolean = false, val observedAt: Long? = null, val error: String? = null, val legacy: Boolean = false)
class NewsRepository(private val api: NovaAtlasApi, private val clock: () -> Long = System::currentTimeMillis) {
    private val lock = Mutex()
    private val mutable = MutableStateFlow(FeedState())
    val state = mutable.asStateFlow()
    private var nextAllowed = 0L
    private var failures = 0
    suspend fun refresh() {
        // Drop concurrent refreshes rather than queue duplicate calls.
        if (!lock.tryLock()) return
        try {
            if (clock() < nextAllowed) return
            mutable.value = mutable.value.copy(refreshing = true)
            var legacy = false
            val articles = try {
                val result = api.articles()
                if (result.status != "ok") throw ApiFailure("format")
                result.articles.map { it.legacy() }
            } catch (e: HttpException) {
                // Only unavailable API versions permit fallback, never auth/429/parse/5xx.
                if (e.code() !in listOf(404,405)) throw e
                legacy = true
                val result = api.getArticles()
                if (result.status != "ok") throw ApiFailure("format")
                result.articles
            }
            val categories = if (!legacy) {
                try { api.site().also { if (it.status != "ok") throw ApiFailure("format") }.categories }
                catch (e: HttpException) { if (e.code() !in listOf(404,405)) throw e; emptyList() }
            } else emptyList()
            if (articles.size > 50 || categories.size > 500) throw ApiFailure("format")
            mutable.value = FeedState(articles, categories, observedAt = clock(), legacy = legacy)
            failures = 0; nextAllowed = clock() + 5_000 // manual debounce
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            failures = (failures + 1).coerceAtMost(5)
            val kind = when (e) {
                is HttpException -> when (e.code()) { 401,403 -> "access_denied"; 429 -> "rate_limited"; in 500..599 -> "server_unavailable"; else -> "http_error" }
                is SerializationException, is IllegalArgumentException, is ResponseFormatLimit -> "format"
                is IOException -> "network"
                else -> "format"
            }
            val seconds = if (e is HttpException && e.code() == 429) retryAfter(e.response()?.headers()?.get("Retry-After"),clock()) else null
            val backoff = max(seconds ?: 0, (30L shl failures).coerceAtMost(900))
            nextAllowed = if (backoff > (Long.MAX_VALUE-clock())/1000) Long.MAX_VALUE else clock()+backoff*1000
            mutable.value = mutable.value.copy(error=kind)
        } finally {
            mutable.value = mutable.value.copy(refreshing=false)
            lock.unlock()
        }
    }
    suspend fun observe(interval: Long = 60_000) {
        require(interval in 30_000..300_000)
        while (true) { refresh(); delay(interval) }
    }
}
