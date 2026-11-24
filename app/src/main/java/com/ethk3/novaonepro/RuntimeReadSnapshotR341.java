package com.ethk3.novaonepro;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Advisory control reads. Persisted decisions and writer fences remain authoritative. */
final class RuntimeReadSnapshotR341 {
    enum Status { NEEDS_FREEZE, FROZEN, NO_DRAW, EXPIRED, UNKNOWN_NEEDS_REFRESH, RESOURCE_WAIT }

    static final class Snapshot {
        final Object scope;
        final String base, target;
        final long sourceCommitAt, deadlineElapsed, expiresAt;
        final Status status;

        Snapshot(Object scope, String base, String target, long sourceCommitAt,
                 long deadlineElapsed, long expiresAt, Status status) {
            this.scope = scope;
            this.base = base;
            this.target = target;
            this.sourceCommitAt = sourceCommitAt;
            this.deadlineElapsed = deadlineElapsed;
            this.expiresAt = expiresAt;
            this.status = status;
        }

        Status statusAt(long now) {
            if (status == Status.NEEDS_FREEZE && now >= deadlineElapsed) return Status.EXPIRED;
            return now >= expiresAt && status != Status.EXPIRED ? Status.UNKNOWN_NEEDS_REFRESH : status;
        }
    }

    interface Loader { Snapshot load(); }

    private final AtomicReference<Snapshot> latest = new AtomicReference<>();
    private final AtomicBoolean refreshing = new AtomicBoolean();

    Status read(Object scope, long now) {
        Snapshot current = latest.get();
        return current == null || current.scope != scope ? Status.UNKNOWN_NEEDS_REFRESH : current.statusAt(now);
    }

    void publishAfterCommit(Snapshot incoming) {
        latest.updateAndGet(current -> {
            if (current == null || current.scope != incoming.scope || current.base.isEmpty()) return incoming;
            if (TargetIssueResolver.compare(incoming.base, current.base) < 0) return current;
            if (current.base.equals(incoming.base) && current.target.equals(incoming.target)
                    && current.status == Status.FROZEN && incoming.status == Status.NEEDS_FREEZE) return current;
            return incoming;
        });
    }

    void freezeCommitted(Object scope, String base, String target, long wall, long mono) {
        publishAfterCommit(new Snapshot(scope, base, target, wall, 0L, mono + 5000L, Status.FROZEN));
    }

    void refreshAsync(Object scope, Executor executor, Loader loader) {
        if (!refreshing.compareAndSet(false, true)) return;
        Snapshot expected = latest.get();
        if (expected == null || expected.scope != scope) {
            Snapshot unknown = unknown(scope);
            if (!latest.compareAndSet(expected, unknown)) { refreshing.set(false); return; }
            expected = unknown;
        }
        final Snapshot anchor = expected;
        try {
            executor.execute(() -> {
                try {
                    Snapshot loaded = loader.load();
                    if (loaded == null || loaded.scope != scope) return;
                    if (!anchor.base.isEmpty() && (loaded.base.isEmpty() || TargetIssueResolver.compare(loaded.base, anchor.base) < 0)) {
                        if (loaded.status != Status.RESOURCE_WAIT || anchor.statusAt(TimeSourceR811.monoNowMs()) == Status.NEEDS_FREEZE)
                            return;
                    }
                    if (anchor.status == Status.FROZEN && anchor.target.equals(loaded.target) && loaded.status == Status.NEEDS_FREEZE)
                        return;
                    latest.compareAndSet(anchor, loaded);
                } catch (RuntimeException failure) {
                    Error fatal = com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(failure);
                    if (fatal != null) throw fatal;
                    latest.compareAndSet(anchor, new Snapshot(scope, anchor.base, anchor.target, anchor.sourceCommitAt,
                            anchor.deadlineElapsed, TimeSourceR811.monoNowMs() + 1000L, Status.RESOURCE_WAIT));
                } finally { refreshing.set(false); }
            });
        } catch (RejectedExecutionException stopped) { refreshing.set(false); }
    }

    void invalidate() { latest.set(null); }

    private static Snapshot unknown(Object scope) {
        return new Snapshot(scope, "", "", 0L, 0L, 0L, Status.UNKNOWN_NEEDS_REFRESH);
    }
}
