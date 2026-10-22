package com.ethk3.novaonepro;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-wide R3.6 analysis owner. It owns only in-memory execution state; durable request truth
 * belongs to AnalysisRequestStoreR36.
 */
public final class AnalysisSingleFlightR36 {
    private static final AnalysisSingleFlightR36 INSTANCE = new AnalysisSingleFlightR36();
    private static final AtomicLong EXECUTION_SEQUENCE = new AtomicLong(0L);
    private final Object lock = new Object();
    private Lease owner;
    private Request latestPending;
    private int active;
    private int maximum;

    private AnalysisSingleFlightR36() {}

    public static AnalysisSingleFlightR36 shared() {
        return INSTANCE;
    }

    public SubmitResult submit(Request request) {
        if (request == null || !request.valid())
            throw new IllegalArgumentException("R36_REQUEST_INVALID");
        synchronized (lock) {
            if (owner == null) {
                owner = Lease.acquire(request);
                RealtimePressureGateR811.beginAnalysis();
                RealtimePressureGateR811.beginCrossProcess(owner.ownerId,owner.request.generation);
                active++;
                maximum = Math.max(maximum, active);
                if (active != 1) throw new IllegalStateException("R36_ACTIVE_NE_1");
                return SubmitResult.start(owner);
            }
            if (!owner.targetIssue.equals(request.targetIssue)
                    && (latestPending == null
                            || TargetIssueResolver.compare(
                                            request.targetIssue, latestPending.targetIssue)
                                    > 0)) {
                latestPending = request;
            }
            if (TargetIssueResolver.compare(request.targetIssue, owner.targetIssue) > 0) {
                owner.token.requestCancelOnce("NEWER_TARGET:" + request.targetIssue);
            }
            return SubmitResult.queued(
                    owner.targetIssue, latestPending == null ? "" : latestPending.targetIssue);
        }
    }

    public Request releaseAndTakeNext(Lease lease) {
        synchronized (lock) {
            if (owner != lease) return null;
            RealtimePressureGateR811.endCrossProcess(lease.ownerId,lease.request.generation);
            owner = null;
            active = Math.max(0, active - 1);
            RealtimePressureGateR811.endAnalysis();
            Request next = latestPending;
            latestPending = null;
            return next;
        }
    }

    /** Cooperative cancellation only; it never creates a replacement owner. */
    public boolean cancelIfNoProgress(long elapsedClockInputMs) {
        final long nowMonoMs = normalizeElapsedClockInput(elapsedClockInputMs);
        synchronized (lock) {
            if (owner == null || owner.token.isCanonicalCommitted()) return false;
            long idle = Math.max(0L, nowMonoMs - owner.token.lastProgressAt());
            return idle >= AppConfig.R36_NO_PROGRESS_CANCEL_MS
                    && owner.token.requestCancelOnce("NO_PROGRESS_" + idle + "MS");
        }
    }

    public Lease currentLease() {
        synchronized (lock) {
            return owner;
        }
    }

    /**
     * Releases an execution owner only when its bound worker is proven dead (or the owning
     * HandlerThread is proven dead before binding). This is not timeout-based parallel rescue:
     * the old execution is physically incapable of continuing before the active count is lowered.
     */
    public ReapResult reapDeadWorker(long elapsedClockInputMs, boolean owningHandlerThreadDead) {
        final long nowMonoMs = normalizeElapsedClockInput(elapsedClockInputMs);
        synchronized (lock) {
            if (owner == null) return ReapResult.none("NO_OWNER");
            if (owner.token.isCanonicalCommitted())
                return ReapResult.none("CANONICAL_COMMITTED_AWAIT_FINALLY");
            Thread worker = owner.token.workerThread();
            boolean boundWorkerDead = worker != null && !worker.isAlive();
            boolean queuedOnDeadHandler =
                    worker == null
                            && owningHandlerThreadDead
                            && nowMonoMs - owner.acquiredMonoAt >= 1000L;
            if (!boundWorkerDead && !queuedOnDeadHandler)
                return ReapResult.none(worker == null ? "WORKER_NOT_BOUND" : "WORKER_ALIVE");
            Lease released = owner;
            RealtimePressureGateR811.endCrossProcess(released.ownerId,released.request.generation);
            released.token.requestCancelOnce(
                    boundWorkerDead ? "BOUND_WORKER_DIED" : "OWNING_HANDLER_DIED_BEFORE_BIND");
            owner = null;
            active = Math.max(0, active - 1);
            RealtimePressureGateR811.endAnalysis();
            Request next = latestPending;
            latestPending = null;
            return new ReapResult(true, "DEAD_WORKER_REAPED", released, next);
        }
    }

    public int activeCount() {
        synchronized (lock) {
            return active;
        }
    }

    public int maximumObserved() {
        synchronized (lock) {
            return maximum;
        }
    }

    public String pendingTarget() {
        synchronized (lock) {
            return latestPending == null ? "" : latestPending.targetIssue;
        }
    }

    /** Package-private deterministic reset used only by host tests. */
    void resetForTests() {
        synchronized (lock) {
            if (owner != null) RealtimePressureGateR811.endAnalysis();
            owner = null;
            latestPending = null;
            active = 0;
            maximum = 0;
        }
    }

    public static final class Request {
        public final String baseIssue, targetIssue, reason, requestHash, historyHash;
        public final long generation, requestAt, deadlineAt, requestMonoAt, deadlineMonoAt;

        private static final java.util.LinkedHashMap<String,Long> DEADLINE_MONO_F01 =
                new java.util.LinkedHashMap<String,Long>();
        private static synchronized long freezeDeadlineMonoF01(String base,String target,long wall,long candidate) {
            String key=base+"|"+target+"|"+wall;
            Long previous=DEADLINE_MONO_F01.get(key);
            long fixed=previous==null?candidate:Math.min(previous.longValue(),candidate);
            DEADLINE_MONO_F01.put(key,fixed);
            while(DEADLINE_MONO_F01.size()>128)DEADLINE_MONO_F01.remove(DEADLINE_MONO_F01.keySet().iterator().next());
            return fixed;
        }
        private Request(Request original,String hash,long fixedWall) {
            baseIssue=original.baseIssue;targetIssue=original.targetIssue;generation=original.generation;
            requestAt=original.requestAt;deadlineAt=fixedWall;requestMonoAt=original.requestMonoAt;
            long candidate=fixedWall<=0L?0L:monotonicNowMs()+Math.max(0L,fixedWall-System.currentTimeMillis());
            deadlineMonoAt=fixedWall==original.deadlineAt?original.deadlineMonoAt:
                    freezeDeadlineMonoF01(baseIssue,targetIssue,fixedWall,Math.min(original.deadlineMonoAt,candidate));
            reason=original.reason;requestHash=original.requestHash;historyHash=safe(hash);
        }
        public Request withDeadlineAtF01(long fixedWall) {
            if(fixedWall<=0L)throw new IllegalArgumentException("F01_REQUEST_FIXED_DEADLINE_INVALID");
            if(fixedWall==deadlineAt)return this;
            return new Request(this,historyHash,fixedWall);
        }

        public Request(
                String base,
                String target,
                long generation,
                long requestAt,
                long deadlineAt,
                String reason,
                String requestHash,
                String historyHash) {
            baseIssue = safe(base);
            targetIssue = safe(target);
            this.generation = generation;
            this.requestAt = requestAt;
            this.deadlineAt = deadlineAt;
            long wallNow = System.currentTimeMillis();
            long monoNow = monotonicNowMs();
            long age = requestAt <= 0L ? 0L : Math.max(0L, wallNow - requestAt);
            requestMonoAt = Math.max(0L, monoNow - age);
            deadlineMonoAt = deadlineAt <= 0L ? 0L : freezeDeadlineMonoF01(baseIssue,targetIssue,deadlineAt,
                    monoNow + Math.max(0L, deadlineAt - wallNow));
            this.reason = safe(reason);
            this.requestHash = safe(requestHash);
            this.historyHash = safe(historyHash);
        }

        public boolean valid() {
            TargetIssueResolver.Resolution x = TargetIssueResolver.exact(baseIssue, targetIssue);
            return x.valid && generation > 0L && requestAt > 0L && requestHash.length() == 64;
        }

        public String attemptId() {
            return requestHash.substring(0, 24) + "-" + generation;
        }

        public Request withHistoryHash(String hash) {
            return new Request(this,safe(hash),deadlineAt);
        }
    }

    public static final class Lease {
        public final Request request;
        public final String baseIssue, targetIssue, ownerId;
        public final long acquiredAt, acquiredMonoAt;
        public final CancellationToken token;

        private Lease(Request request) {
            this.request = request;
            baseIssue = request.baseIssue;
            targetIssue = request.targetIssue;
            acquiredAt = System.currentTimeMillis();
            acquiredMonoAt = monotonicNowMs();
            long sequence = EXECUTION_SEQUENCE.incrementAndGet();
            ownerId =
                    V51ChecksumEngine.sha256(
                                    "R366_EXECUTION_LEASE|"
                                            + request.attemptId()
                                            + '|'
                                            + acquiredAt
                                            + '|'
                                            + acquiredMonoAt
                                            + '|'
                                            + sequence
                                            + '|'
                                            + Thread.currentThread().getId())
                            .substring(0, 32);
            token = new CancellationToken(targetIssue, acquiredMonoAt);
            token.pressureOwnerCP4=ownerId;token.pressureGenerationCP4=request.generation;
        }

        static Lease acquire(Request request) {
            return new Lease(request);
        }

        public void bindCurrentThread() {
            token.bindWorker(Thread.currentThread());
        }
    }

    public static final class ReapResult {
        public final boolean reaped;
        public final String status;
        public final Lease released;
        public final Request next;

        ReapResult(boolean reaped, String status, Lease released, Request next) {
            this.reaped = reaped;
            this.status = safe(status);
            this.released = released;
            this.next = next;
        }

        static ReapResult none(String status) {
            return new ReapResult(false, status, null, null);
        }
    }

    public static final class SubmitResult {
        public final boolean startNow, queued;
        public final Lease lease;
        public final String ownerTarget, pendingTarget;

        private SubmitResult(
                boolean start, boolean queued, Lease lease, String owner, String pending) {
            startNow = start;
            this.queued = queued;
            this.lease = lease;
            ownerTarget = safe(owner);
            pendingTarget = safe(pending);
        }

        static SubmitResult start(Lease lease) {
            return new SubmitResult(true, false, lease, lease.targetIssue, "");
        }

        static SubmitResult queued(String owner, String pending) {
            return new SubmitResult(false, true, null, owner, pending);
        }
    }

    public static final class CancellationToken {
        private final String targetIssue;
        private boolean cancelRequested, canonicalCommitted;
        private boolean finalizing;
        private String cancelReason = "", stage = "ACQUIRED";
        private long lastProgressMonoAt, lastDurablePulseWallAt;
        private Thread workerThread;
        private String pressureOwnerCP4="";
        private long pressureGenerationCP4;

        CancellationToken(String target, long now) {
            targetIssue = safe(target);
            lastProgressMonoAt = now;
            lastDurablePulseWallAt = System.currentTimeMillis();
        }

        public synchronized void pulse(String value) {
            if (canonicalCommitted) return;
            stage = safe(value);
            lastProgressMonoAt = monotonicNowMs();
        }

        synchronized void bindWorker(Thread worker) {
            if (worker == null) throw new IllegalArgumentException("R368_WORKER_NULL");
            if (workerThread != null && workerThread != worker && workerThread.isAlive())
                throw new IllegalStateException("R368_WORKER_REBIND_FORBIDDEN");
            workerThread = worker;
            stage = "WORKER_BOUND";
            lastProgressMonoAt = monotonicNowMs();
        }

        synchronized Thread workerThread() {
            return workerThread;
        }

        public synchronized boolean requestCancelOnce(String reason) {
            if (canonicalCommitted || cancelRequested) return false;
            String normalized = safe(reason);
            if (finalizing && normalized.startsWith("NEWER_TARGET:")) return false;
            cancelRequested = true;
            cancelReason = normalized;
            return true;
        }

        /** Linearization point: only a later supersede is deferred, never a hard failure. */
        public synchronized void enterFinalization() {
            if (canonicalCommitted) return;
            if (cancelRequested) {
                throw new CancelledBeforeCommitException(targetIssue, stage, cancelReason);
            }
            finalizing = true;
            stage = "PREDRAW_FINALIZING";
            lastProgressMonoAt = monotonicNowMs();
        }

        public synchronized boolean isFinalizing() {
            return finalizing && !canonicalCommitted;
        }

        public synchronized void checkpoint(String value) {
            RealtimePressureGateR811.renewCrossProcess(pressureOwnerCP4,pressureGenerationCP4);
            if (!canonicalCommitted) RuntimeResourceAuditR927.checkpoint(value);
            pulse(value);
            if (cancelRequested && !canonicalCommitted) {
                throw new CancelledBeforeCommitException(targetIssue, stage, cancelReason);
            }
        }

        public synchronized void markCanonicalCommitted() {
            canonicalCommitted = true;
            cancelRequested = false;
            stage = "CANONICAL_COMMITTED";
            lastProgressMonoAt = monotonicNowMs();
        }

        public synchronized boolean isCanonicalCommitted() {
            return canonicalCommitted;
        }

        public synchronized long lastProgressAt() {
            return lastProgressMonoAt;
        }

        /** Coalesces SQLite lease writes while every in-memory cancellation checkpoint remains. */
        public synchronized boolean claimDurablePulse(long now, boolean force) {
            if (!force && Math.max(0L, now - lastDurablePulseWallAt) < 10000L) return false;
            lastDurablePulseWallAt = now;
            return true;
        }

        public synchronized String stage() {
            return stage;
        }

        public synchronized String cancelReason() {
            return cancelReason;
        }

        public synchronized boolean supersededByNewerTarget() {
            return cancelReason.startsWith("NEWER_TARGET:");
        }
    }

    public static final class CancelledBeforeCommitException extends RuntimeException {
        CancelledBeforeCommitException(String target, String stage, String reason) {
            super(
                    "R36_CANCELLED_BEFORE_COMMIT|target="
                            + safe(target)
                            + "|stage="
                            + safe(stage)
                            + "|reason="
                            + safe(reason));
        }
    }

    /**
     * Compatibility bridge for retained callers that historically passed wall-clock epoch millis.
     * New elapsed decisions are always made in the monotonic clock domain; small synthetic values
     * remain accepted so deterministic host harnesses can advance time without sleeping.
     */
    private static long normalizeElapsedClockInput(long candidateMs) {
        if (candidateMs <= 0L) return monotonicNowMs();
        // Epoch millis is currently ~1e12 while elapsed-realtime milliseconds are far smaller.
        // Treat values beyond ten years as wall-clock input and never subtract them from mono state.
        if (candidateMs > 315360000000L) return monotonicNowMs();
        return candidateMs;
    }

    /** Host-safe and Android-safe monotonic elapsed clock; never use it as persisted epoch time. */
    private static long monotonicNowMs() {
        return Math.max(0L, TimeSourceR811.monoNowMs());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
