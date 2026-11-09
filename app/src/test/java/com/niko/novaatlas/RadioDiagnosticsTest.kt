package com.niko.novaatlas
import org.junit.Test
import org.junit.Assert.*
class RadioDiagnosticsTest {
    @Test fun observationsNeverClaimAudience() {
        var now=0L;val d=RadioDiagnostics{now}
        d.record(RadioObservation.PLAY_REQUESTED);now=5;d.record(RadioObservation.CONNECTION_OPEN);d.transferred(10);d.transferred(20)
        val s=d.snapshot();assertEquals(30L,s.network_bytes);assertEquals(1L,s.counts[RadioObservation.FIRST_NETWORK_BYTES]);assertEquals("NOT_TESTED",s.audience_delivery)
        assertFalse(d.export().contains("https:"));assertEquals(5L,s.events.last().elapsed_ms)
    }
    @Test fun boundedCountersPreserveEvidenceNotStrings() {
        val d=RadioDiagnostics();repeat(1000){d.record(RadioObservation.BUFFERING)}
        assertEquals(128,d.snapshot().events.size);assertEquals(1000L,d.snapshot().counts[RadioObservation.BUFFERING])
    }
    @Test fun newConnectionSeparatesFirstBytesAndPause() {
        val d=RadioDiagnostics();repeat(2){d.record(RadioObservation.CONNECTION_OPEN);d.transferred(2)}
        d.record(RadioObservation.PAUSE_REQUESTED)
        assertEquals(2L,d.snapshot().counts[RadioObservation.FIRST_NETWORK_BYTES]);assertEquals(RadioObservation.PAUSE_REQUESTED,d.snapshot().events.last().kind)
    }
    @Test fun invalidValuesRejected() { try {RadioDiagnostics().record(RadioObservation.TERMINAL_ERROR,-1);fail()} catch (_:IllegalArgumentException){} }
}
