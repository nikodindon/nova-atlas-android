package com.niko.novaatlas

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable enum class RadioObservation {
    PLAYER_CREATED, PLAYER_RELEASED, PLAY_REQUESTED, PAUSE_REQUESTED,
    SESSION_PLAY_INTENT, SESSION_PAUSE_INTENT, BUFFERING, CONNECTION_OPEN,
    FIRST_NETWORK_BYTES, NETWORK_TRANSFER_ENDED, LOCAL_PLAYBACK_STARTED,
    LOCAL_PLAYBACK_STOPPED, RECONNECT_SCHEDULED, TERMINAL_ERROR, CLEAN_END,
}
@Serializable data class RadioObservationRecord(val sequence: Long, val elapsed_ms: Long, val kind: RadioObservation, val value: Long? = null)
@Serializable data class RadioDiagnosticSnapshot(val version: Int = 1, val events: List<RadioObservationRecord>, val counts: Map<RadioObservation,Long>, val network_bytes: Long, val audience_delivery: String = "NOT_TESTED")

/** Process-local bounded numeric observations only: no URI, ID, SSID, exception or text input. */
class RadioDiagnostics(private val clock: () -> Long = { System.nanoTime()/1_000_000 }) {
    private val started=clock()
    private val events=ArrayDeque<RadioObservationRecord>()
    private val counts=mutableMapOf<RadioObservation,Long>()
    private var sequence=0L
    private var bytes=0L
    private var firstBytes=false
    @Synchronized fun record(kind: RadioObservation, value: Long? = null) {
        require(value == null || value >= 0)
        sequence++
        counts[kind]=(counts[kind] ?: 0)+1
        if (events.size == 128) events.removeFirst()
        events.addLast(RadioObservationRecord(sequence,(clock()-started).coerceAtLeast(0),kind,value))
        if (kind == RadioObservation.CONNECTION_OPEN) firstBytes=false
    }
    @Synchronized fun transferred(count: Int) {
        if (count<=0) return
        bytes=if (Long.MAX_VALUE-bytes<count) Long.MAX_VALUE else bytes+count
        if (!firstBytes) { firstBytes=true;record(RadioObservation.FIRST_NETWORK_BYTES,count.toLong()) }
    }
    @Synchronized fun snapshot()=RadioDiagnosticSnapshot(events=events.toList(),counts=counts.toMap(),network_bytes=bytes)
    fun export()=Json.encodeToString(snapshot())
}
object RadioTelemetry { val observations=RadioDiagnostics() }
