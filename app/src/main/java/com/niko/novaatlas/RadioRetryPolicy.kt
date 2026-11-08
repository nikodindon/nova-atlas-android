package com.niko.novaatlas

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import javax.net.ssl.SSLException
import java.io.IOException

fun radioRetryDelay(count: Int, status: Int? = null, retry: String? = null, now: Long = System.currentTimeMillis(), tls: Boolean = false): Long {
    if (count !in 1..4 || tls || status in listOf(401,402,403,404,410)) return C.TIME_UNSET
    val delay = (3_000L shl (count-1)).coerceAtMost(30_000)
    val imposed = retryAfter(retry,now)
    // Long embargo: stop, don't shorten it or leave a multi-hour sleeping load.
    if (imposed != null && imposed > 60) return C.TIME_UNSET
    return maxOf(delay, (imposed ?: 0)*1000)
}
@UnstableApi
class RadioRetryPolicy: DefaultLoadErrorHandlingPolicy(4) {
    override fun getRetryDelayMsFor(info: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val error = info.exception
        val http = error as? HttpDataSource.InvalidResponseCodeException
        var cause: Throwable? = error
        var tls = false
        repeat(8) { if (cause is SSLException) tls=true; cause=cause?.cause }
        return radioRetryDelay(info.errorCount,http?.responseCode,http?.headerFields?.entries?.firstOrNull { it.key.equals("Retry-After",true) }?.value?.firstOrNull(),tls=tls)
    }
}
