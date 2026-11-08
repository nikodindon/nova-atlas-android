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
class RadioEmbargo(private val clock: () -> Long = System::currentTimeMillis) {
    private val deadline = java.util.concurrent.atomic.AtomicLong(0)
    fun record(value: String?) {
        val seconds = retryAfter(value,clock()) ?: return
        val now=clock()
        val until=if (seconds > (Long.MAX_VALUE-now)/1000) Long.MAX_VALUE else now+seconds*1000
        deadline.updateAndGet { maxOf(it,until) }
    }
    fun allowed() = clock() >= deadline.get()
}
@androidx.annotation.OptIn(UnstableApi::class)
class RadioRetryPolicy(private val embargo: RadioEmbargo = RadioEmbargo()): DefaultLoadErrorHandlingPolicy(4) {
    override fun getRetryDelayMsFor(info: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val error = info.exception
        val http = error as? HttpDataSource.InvalidResponseCodeException
        var cause: Throwable? = error
        var tls = false
        repeat(8) { if (cause is SSLException) tls=true; cause=cause?.cause }
        val header=http?.headerFields?.entries?.firstOrNull { it.key.equals("Retry-After",true) }?.value?.firstOrNull()
        if (http?.responseCode == 429) embargo.record(header)
        return radioRetryDelay(info.errorCount,http?.responseCode,header,tls=tls)
    }
}

/** EOF is distinct from HTTP load errors: at most four prepares per user session. */
class RadioEndRecovery {
    private var attempts=0
    fun reset() { attempts=0 }
    fun nextDelay(): Long? { if (attempts>=4) return null; return 3_000L shl attempts++ }
}
