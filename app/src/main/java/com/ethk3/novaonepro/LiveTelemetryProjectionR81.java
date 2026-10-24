package com.ethk3.novaonepro;

import java.util.Map;

/** Optional UI/diagnostic projections on live owners; never leases, cursors or eligibility.
 * The bound covers writer admission only. SQLite transaction duration is still measured
 * by the existing coordinator; no latency or success is fabricated when a projection drops.
 * 2ms also expired on uncontended admission (deadline check after lock acquisition), so
 * heartbeats dropped on slow devices; 20ms still never waits out a held writer slice.
 */
final class LiveTelemetryProjectionR81 {
    static final long MAX_WRITER_WAIT_MS = 20L;
    private LiveTelemetryProjectionR81() {}

    static boolean tryWrite(DbStore store, Map<String,String> values) {
        if (store == null) return false;
        if (values == null || values.isEmpty()) return true;
        try (DatabaseCoordinator.WriterDeadlineScopeCP3 ignored =
                DatabaseCoordinator.withWriterDeadlineCP3(
                        TimeSourceR811.monoNowMs() + MAX_WRITER_WAIT_MS)) {
            return store.trySetTelemetryStatesR77(values);
        } catch (RuntimeException unavailable) {
            // The authoritative request/claim/commit remains owned by its existing API.
            return false;
        }
    }
}
