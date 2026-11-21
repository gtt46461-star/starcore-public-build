package com.ethk3.novaonepro;

/** Capture immediately after the original transaction owner returns. A later
 * telemetry callback must not sample its own execution time as the commit time.
 * This is a return observation, not a measurement of the SQLite commit instant.
 */
public final class DurableReturnTimingR334 {
    public final long startedWallMs, returnedWallMs, elapsedMs;
    public DurableReturnTimingR334(long startedWallMs, long startedMonoMs,
                                  long returnedWallMs, long returnedMonoMs) {
        this.startedWallMs = startedWallMs;
        this.returnedWallMs = returnedWallMs;
        this.elapsedMs = startedMonoMs < 0L || returnedMonoMs < startedMonoMs
                ? -1L : returnedMonoMs - startedMonoMs;
    }
}
