package com.ethk3.novaonepro;

import android.content.Context;
import android.os.PowerManager;

/** One bounded PIT-to-canonical-commit lease; it cannot be renewed by service heartbeats. */
public final class PredrawWakeLeaseR81 implements AutoCloseable {
    public static final long MAX_HOLD_MS = 18000L;
    private PowerManager.WakeLock held;
    private boolean closed;
    private PredrawWakeLeaseR81(PowerManager.WakeLock value) { held = value; }

    public static PredrawWakeLeaseR81 acquire(Context context, long deadlineMonoAt) {
        long remaining = deadlineMonoAt - TimeSourceR811.monoNowMs();
        if (remaining <= 0L) throw new IllegalStateException("PREDRAW_WAKE_DEADLINE_EXPIRED");
        PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (manager == null) throw new IllegalStateException("PREDRAW_POWER_SERVICE_UNAVAILABLE");
        PowerManager.WakeLock lock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                AppConfig.PROJECT_NAME + ":PIT_CANONICAL_R81");
        lock.setReferenceCounted(false);
        try {
            lock.acquire(Math.min(MAX_HOLD_MS, remaining));
            return new PredrawWakeLeaseR81(lock);
        } catch (RuntimeException failure) {
            try { if (lock.isHeld()) lock.release(); } catch (RuntimeException releaseFailure) { failure.addSuppressed(releaseFailure); }
            throw failure;
        }
    }

    /** Call immediately after the durable canonical result, before optional diagnostics. */
    public void canonicalCommitted() { close(); }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        PowerManager.WakeLock lock = held; held = null;
        if (lock != null) {
            // The OS timeout can race this explicit release. That race must not turn
            // an already durable canonical commit into a reported prediction failure.
            try { if (lock.isHeld()) lock.release(); } catch (RuntimeException alreadyTimedOut) { }
        }
    }
}
